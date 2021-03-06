package gzx.devops.common.commander;

import cn.hutool.cache.impl.LRUCache;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.JarClassLoader;
import cn.hutool.core.text.StrSpliter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.sun.tools.attach.VirtualMachine;
import gzx.devops.util.FileUtils;
import gzx.devops.common.commander.impl.LinuxProjectCommander;
import gzx.devops.common.commander.impl.WindowsProjectCommander;
import gzx.devops.model.RunMode;
import gzx.devops.model.data.JdkInfoModel;
import gzx.devops.model.data.ProjectInfoModel;
import gzx.devops.model.system.NetstatModel;
import gzx.devops.service.manage.JdkInfoService;
import gzx.devops.service.manage.ProjectInfoService;
import gzx.devops.system.AgentExtConfigBean;
import gzx.devops.system.JpomRuntimeException;
import gzx.devops.util.CommandUtil;
import gzx.devops.util.JvmUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * 项目命令执行基类
 *
 * @author Administrator
 */
public abstract class AbstractProjectCommander {

    public static final String RUNNING_TAG = "running";
    public static final String STOP_TAG = "stopped";

    private static AbstractProjectCommander abstractProjectCommander = null;

    /**
     * 进程id 对应名称
     */
    public static final ConcurrentHashMap<Integer, String> PID_DevOps_NAME = new ConcurrentHashMap<>();
    /**
     * 进程Id 获取端口号
     */
    public static final LRUCache<Integer, Integer> PID_PORT = new LRUCache<>(100, TimeUnit.MINUTES.toMillis(10));

    /**
     * 实例化Commander
     *
     * @return 命令执行对象
     */
    public static AbstractProjectCommander getInstance() {
        if (abstractProjectCommander != null) {
            return abstractProjectCommander;
        }
        if (SystemUtil.getOsInfo().isLinux()) {
            // Linux系统
            abstractProjectCommander = new LinuxProjectCommander();
        } else if (SystemUtil.getOsInfo().isWindows()) {
            // Windows系统
            abstractProjectCommander = new WindowsProjectCommander();
        } else if (SystemUtil.getOsInfo().isMac()) {
            abstractProjectCommander = new LinuxProjectCommander();
        } else {
            throw new JpomRuntimeException("不支持的：" + SystemUtil.getOsInfo().getName());
        }
        return abstractProjectCommander;
    }

    //---------------------------------------------------- 基本操作----start

    /**
     * 生成可以执行的命令
     *
     * @param projectInfoModel 项目
     * @return null 是条件不足
     */
    public abstract String buildCommand(ProjectInfoModel projectInfoModel);

    protected String getRunJavaPath(ProjectInfoModel projectInfoModel, boolean w) {
        if (StrUtil.isEmpty(projectInfoModel.getJdkId())) {
            return w ? "javaw" : "java";
        }
        JdkInfoService bean = SpringUtil.getBean(JdkInfoService.class);
        JdkInfoModel item = bean.getItem(projectInfoModel.getJdkId());
        if (item == null) {
            return w ? "javaw" : "java";
        }
        String jdkJavaPath = FileUtils.getJdkJavaPath(item.getPath(), w);
        if (jdkJavaPath.contains(StrUtil.SPACE)) {
            jdkJavaPath = String.format("\"%s\"", jdkJavaPath);
        }
        return jdkJavaPath;
    }

    /**
     * 启动
     *
     * @param projectInfoModel 项目
     * @return 结果
     * @throws Exception 异常
     */
    public String start(ProjectInfoModel projectInfoModel) throws Exception {
        String msg = checkStart(projectInfoModel);
        if (msg != null) {
            return msg;
        }
        String command = buildCommand(projectInfoModel);
        if (command == null) {
            throw new JpomRuntimeException("没有需要执行的命令");
        }
        // 执行命令
        ThreadUtil.execute(() -> {
            try {
                File file = FileUtil.file(projectInfoModel.allLib());
                if (SystemUtil.getOsInfo().isWindows()) {
                    CommandUtil.execSystemCommand(command, file);
                } else {
                    CommandUtil.asyncExeLocalCommand(file, command);
                }
            } catch (Exception e) {
                DefaultSystemLog.getLog().error("执行命令失败", e);
            }
        });
        //
        loopCheckRun(projectInfoModel.getId(), true);
        return status(projectInfoModel.getId());
    }

    /**
     * 查询出指定端口信息
     *
     * @param pid       进程id
     * @param listening 是否只获取检查状态的
     * @return 数组
     */
    public abstract List<NetstatModel> listNetstat(int pid, boolean listening);

    /**
     * 停止
     *
     * @param projectInfoModel 项目
     * @return 结果
     * @throws Exception 异常
     */
    public String stop(ProjectInfoModel projectInfoModel) throws Exception {
        String tag = projectInfoModel.getId();
        String token = projectInfoModel.getToken();
        if (StrUtil.isNotEmpty(token)) {
            try {
                String body = HttpUtil.createGet(token).form("projectId", projectInfoModel.getId()).execute().body();
                DefaultSystemLog.getLog().info(projectInfoModel.getName() + ":" + body);
            } catch (Exception e) {
                DefaultSystemLog.getLog().error("WebHooks 调用错误", e);
                return "WebHooks error:" + e.getMessage();
            }
        }
        // 再次查看进程信息
        String result = status(tag);
        //
        int pid = parsePid(result);
        if (pid > 0) {
            // 清空名称缓存
            PID_DevOps_NAME.remove(pid);
            // 端口号缓存
            PID_PORT.remove(pid);
        }
        return result;
    }

    /**
     * 重启
     *
     * @param projectInfoModel 项目
     * @return 结果
     * @throws Exception 异常
     */
    public String restart(ProjectInfoModel projectInfoModel) throws Exception {
        if (isRun(projectInfoModel.getId())) {
            stop(projectInfoModel);
        }
        return start(projectInfoModel);
    }

    /**
     * 启动项目前基本检查
     *
     * @param projectInfoModel 项目
     * @return null 检查一切正常
     * @throws Exception 异常
     */
    private String checkStart(ProjectInfoModel projectInfoModel) throws Exception {
        int pid = getPid(projectInfoModel.getId());
        if (pid > 0) {
            return "当前程序正常运行中，不能重复启动,PID:" + pid;
        }
        String lib = projectInfoModel.allLib();
        File fileLib = new File(lib);
        File[] files = fileLib.listFiles();
        if (files == null || files.length <= 0) {
            return "没有jar包,请先到文件管理中上传程序的jar";
        }
        //
        if (projectInfoModel.getRunMode() == RunMode.ClassPath) {
            JarClassLoader jarClassLoader = JarClassLoader.load(fileLib);
            // 判断主类
            try {
                jarClassLoader.loadClass(projectInfoModel.getMainClass());
            } catch (ClassNotFoundException notFound) {
                return "没有找到对应的MainClass:" + projectInfoModel.getMainClass();
            }
        } else {
            List<File> fileList = ProjectInfoModel.listJars(projectInfoModel);
            if (fileList.size() <= 0) {
                return String.format("没有%s包,请先到文件管理中上传程序的%s", projectInfoModel.getRunMode().name(), projectInfoModel.getRunMode().name());
            }
            File jarFile = fileList.get(0);
            String checkJar = checkJar(jarFile);
            if (checkJar != null) {
                return checkJar;
            }
        }
        // 备份日志
        backLog(projectInfoModel);
        return null;
    }

    private static String checkJar(File jarFile) {
        try (JarFile jarFile1 = new JarFile(jarFile)) {
            Manifest manifest = jarFile1.getManifest();
            Attributes attributes = manifest.getMainAttributes();
            String mainClass = attributes.getValue(Attributes.Name.MAIN_CLASS);
            if (mainClass == null) {
                return jarFile.getAbsolutePath() + "中没有找到对应的MainClass属性";
            }
            JarClassLoader jarClassLoader = JarClassLoader.load(jarFile);
            try {
                jarClassLoader.loadClass(mainClass);
            } catch (ClassNotFoundException notFound) {
                return jarFile.getAbsolutePath() + "中没有找到对应的MainClass:" + mainClass;
            }
        } catch (Exception e) {
            DefaultSystemLog.getLog().error("解析jar", e);
            return jarFile.getAbsolutePath() + " 解析错误:" + e.getMessage();
        }
        return null;
    }

    /**
     * 清空日志信息
     *
     * @param projectInfoModel 项目
     * @return 结果
     */
    public String backLog(ProjectInfoModel projectInfoModel) {
        if (StrUtil.isEmpty(projectInfoModel.getLog())) {
            return "ok";
        }
        File file = new File(projectInfoModel.getLog());
        if (!file.exists() || file.isDirectory()) {
            return "not exists";
        }
        // 文件内容太少不处理
        if (file.length() <= 1000) {
            return "ok";
        }
        if (AgentExtConfigBean.getInstance().openLogBack()) {
            // 开启日志备份才移动文件
            File backPath = projectInfoModel.getLogBack();
            backPath = new File(backPath, DateTime.now().toString(DatePattern.PURE_DATETIME_FORMAT) + ".log");
            FileUtil.copy(file, backPath, true);
        }
        if (SystemUtil.getOsInfo().isLinux()) {
            CommandUtil.execSystemCommand("cp /dev/null " + projectInfoModel.getLog());
        } else if (SystemUtil.getOsInfo().isWindows()) {
            // 清空日志
            String r = CommandUtil.execSystemCommand("echo  \"\" > " + file.getAbsolutePath());
            if (StrUtil.isEmpty(r)) {
                DefaultSystemLog.getLog().info(r);
            }
        }
        return "ok";
    }

    /**
     * 查看状态
     *
     * @param tag 运行标识
     * @return 查询结果
     * @throws Exception 异常
     */
    public String status(String tag) throws Exception {
        VirtualMachine virtualMachine = JvmUtil.getVirtualMachine(tag);
        if (virtualMachine == null) {
            return getJpsStatus(tag);
        }
        try {
            return StrUtil.format("{}:{}", AbstractProjectCommander.RUNNING_TAG, virtualMachine.id());
        } finally {
            virtualMachine.detach();
        }
    }

    /**
     * 尝试jps 中查看进程id
     *
     * @param tag 进程标识
     * @return 运行标识
     */
    private String getJpsStatus(String tag) {
        String execSystemCommand = CommandUtil.execSystemCommand("jps -mv");
        List<String> list = StrSpliter.splitTrim(execSystemCommand, StrUtil.LF, true);
        for (String item : list) {
            if (JvmUtil.checkCommandLineIsJpom(item, tag)) {
                String[] split = StrUtil.split(item, StrUtil.SPACE);
                return StrUtil.format("{}:{}", AbstractProjectCommander.RUNNING_TAG, split[0]);
            }
        }
        return AbstractProjectCommander.STOP_TAG;
    }

    //---------------------------------------------------- 基本操作----end

    /**
     * 获取进程占用的主要端口
     *
     * @param pid 进程id
     * @return 端口
     */
    public String getMainPort(int pid) {
        Integer cachePort = PID_PORT.get(pid);
        if (cachePort != null) {
            return cachePort.toString();
        }
        List<NetstatModel> list = listNetstat(pid, true);
        if (list == null) {
            return StrUtil.DASHED;
        }
        int port = Integer.MAX_VALUE;
        for (NetstatModel model : list) {
            String local = model.getLocal();
            String portStr = getPortFormLocalIp(local);
            if (portStr == null) {
                continue;
            }
            // 取最小的端口号
            int minPort = Convert.toInt(portStr, Integer.MAX_VALUE);
            if (minPort < port) {
                port = minPort;
            }
        }
        if (port == Integer.MAX_VALUE) {
            return StrUtil.DASHED;
        }
        // 缓存
        PID_PORT.put(pid, port);
        return String.valueOf(port);
    }

    /**
     * 判断ip 信息是否为本地ip
     *
     * @param local ip信息
     * @return true 是本地ip
     */
    private String getPortFormLocalIp(String local) {
        if (StrUtil.isEmpty(local)) {
            return null;
        }
        List<String> ipPort = StrSpliter.splitTrim(local, StrUtil.COLON, true);
        if (ipPort.isEmpty()) {
            return null;
        }
        if ("0.0.0.0".equals(ipPort.get(0)) || ipPort.size() == 1) {
            // 0.0.0.0:8084  || :::18000
            return ipPort.get(ipPort.size() - 1);
        }
        return null;
    }


    /**
     * 根据指定进程id获取系统 名称
     *
     * @param pid 进程id
     * @return false 不是来自DevOps
     * @throws IOException 异常
     */
    public String getJpomNameByPid(int pid) throws IOException {
        String name = PID_DevOps_NAME.get(pid);
        if (name != null) {
            return name;
        }
        ProjectInfoService projectInfoService = SpringUtil.getBean(ProjectInfoService.class);
        List<ProjectInfoModel> projectInfoModels = projectInfoService.list();
        if (projectInfoModels == null || projectInfoModels.isEmpty()) {
            return StrUtil.DASHED;
        }
        VirtualMachine virtualMachine = JvmUtil.getVirtualMachine(pid);
        if (virtualMachine == null) {
            return StrUtil.DASHED;
        }
        try {
            for (ProjectInfoModel projectInfoModel : projectInfoModels) {
                if (JvmUtil.checkVirtualMachineIsJpom(virtualMachine, projectInfoModel.getId())) {
                    name = projectInfoModel.getName();
                    break;
                }
            }
        } finally {
            virtualMachine.detach();
        }
        if (name != null) {
            PID_DevOps_NAME.put(pid, name);
            return name;
        }
        return StrUtil.DASHED;
    }


    /**
     * 获取进程id
     *
     * @param tag 项目Id
     * @return 未运行 返回 0
     * @throws Exception 异常
     */
    public int getPid(String tag) throws Exception {
        String result = status(tag);
        return parsePid(result);
    }

    /**
     * 转换pid
     *
     * @param result 查询信息
     * @return int
     */
    protected static int parsePid(String result) {
        if (result.startsWith(AbstractProjectCommander.RUNNING_TAG)) {
            return Convert.toInt(result.split(":")[1]);
        }
        return 0;
    }

    /**
     * 是否正在运行
     *
     * @param tag id
     * @return true 正在运行
     * @throws Exception 异常
     */
    public boolean isRun(String tag) throws Exception {
        String result = status(tag);
        return result.contains(AbstractProjectCommander.RUNNING_TAG);
    }

    /***
     * 阻塞检查程序状态
     * @param tag 程序tag
     * @param status 要检查的状态
     * @throws Exception 异常
     * @return 和参数status相反
     */
    protected boolean loopCheckRun(String tag, boolean status) throws Exception {
        int count = 0;
        do {
            if (isRun(tag) == status) {
                return status;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        } while (count++ < 20);
        return !status;
    }
}
