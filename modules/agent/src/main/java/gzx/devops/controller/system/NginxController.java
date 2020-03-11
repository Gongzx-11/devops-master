package gzx.devops.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSpliter;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.odiszapc.nginxparser.NgxBlock;
import com.github.odiszapc.nginxparser.NgxConfig;
import com.github.odiszapc.nginxparser.NgxEntry;
import com.github.odiszapc.nginxparser.NgxParam;
import gzx.devops.common.BaseAgentController;
import gzx.devops.common.commander.AbstractSystemCommander;
import gzx.devops.service.WhitelistDirectoryService;
import gzx.devops.service.system.NginxService;
import gzx.devops.util.CommandUtil;
import gzx.devops.util.StringUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * nginx 列表
 *
 */
@RestController
@RequestMapping("/system/nginx")
public class NginxController extends BaseAgentController {

    @Resource
    private NginxService nginxService;
    @Resource
    private WhitelistDirectoryService whitelistDirectoryService;

    /**
     * 配置列表
     *
     * @return json
     */
    @RequestMapping(value = "list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String list(String whitePath, String name) {
        if (whitelistDirectoryService.checkNgxDirectory(whitePath)) {
            if (StrUtil.isEmpty(name)) {
                name = "/";
            }
            String newName = pathSafe(name);
            JSONArray array = nginxService.list(whitePath, newName);
            return JsonMessage.getString(200, "", array);
        }
        return JsonMessage.getString(400, "文件路径错误");
    }

    /**
     * nginx列表
     */
    @RequestMapping(value = "tree.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String tree() {
        JSONArray array = nginxService.tree();
        return JsonMessage.getString(200, "", array);
    }

    /**
     * 获取配置文件信息页面
     *
     * @param path 白名单路径
     * @param name 名称
     * @return 页面
     */
    @RequestMapping(value = "item_data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String itemData(String path, String name) {
        String newName = pathSafe(name);
        if (whitelistDirectoryService.checkNgxDirectory(path)) {
            File file = FileUtil.file(path, newName);
            JSONObject jsonObject = new JSONObject();
            String string = FileUtil.readUtf8String(file);
            jsonObject.put("context", string);
            String rName = StringUtil.delStartPath(file, path, true);
            // nginxService.paresName(path, file.getAbsolutePath())
            jsonObject.put("name", rName);
            jsonObject.put("whitePath", path);
            return JsonMessage.getString(200, "", jsonObject);
//            setAttribute("data", jsonObject);
        }
        return JsonMessage.getString(400, "错误");
    }

    /**
     * 新增或修改配置
     *
     * @param name      文件名
     * @param whitePath 白名单路径
     * @param genre     操作类型
     * @return json
     */
    @RequestMapping(value = "updateNgx", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String updateNgx(String name, String whitePath, String genre) {
        if (StrUtil.isEmpty(name)) {
            return JsonMessage.getString(400, "请填写文件名");
        }
        if (!name.endsWith(".conf")) {
            return JsonMessage.getString(400, "文件后缀必须为\".conf\"");
        }
        if (!checkPathSafe(name)) {
            return JsonMessage.getString(400, "文件名存在非法字符");
        }
        if (!whitelistDirectoryService.checkNgxDirectory(whitePath)) {
            return JsonMessage.getString(400, "请选择正确的白名单");
        }
        //nginx文件
        File file = FileUtil.file(whitePath, name);
        if ("add".equals(genre) && file.exists()) {
            return JsonMessage.getString(400, "该文件已存在");
        }
        String context = getUnescapeParameter("context");
        if (StrUtil.isEmpty(context)) {
            return JsonMessage.getString(400, "请填写配置信息");
        }
        InputStream inputStream = new ByteArrayInputStream(context.getBytes());
        try {
            NgxConfig conf = NgxConfig.read(inputStream);
            List<NgxEntry> list = conf.findAll(NgxBlock.class, "server");
            if (list == null || list.size() <= 0) {
                return JsonMessage.getString(404, "内容解析为空");
            }
            for (NgxEntry ngxEntry : list) {
                NgxBlock ngxBlock = (NgxBlock) ngxEntry;
                // 检查日志路径
                NgxParam accessLog = ngxBlock.findParam("access_log");
                if (accessLog != null) {
                    FileUtil.mkParentDirs(accessLog.getValue());
                }
                accessLog = ngxBlock.findParam("error_log");
                if (accessLog != null) {
                    FileUtil.mkParentDirs(accessLog.getValue());
                }
                // 检查证书文件
                NgxParam sslCertificate = ngxBlock.findParam("ssl_certificate");
                if (sslCertificate != null && !FileUtil.exist(sslCertificate.getValue())) {
                    return JsonMessage.getString(404, "证书文件ssl_certificate,不存在");
                }
                NgxParam sslCertificateKey = ngxBlock.findParam("ssl_certificate_key");
                if (sslCertificateKey != null && !FileUtil.exist(sslCertificateKey.getValue())) {
                    return JsonMessage.getString(404, "证书文件ssl_certificate_key,不存在");
                }
                if (!checkRootRole(ngxBlock)) {
                    return JsonMessage.getString(405, "非系统管理员，不能配置静态资源代理");
                }
            }
        } catch (IOException e) {
            DefaultSystemLog.getLog().error("解析失败", e);
            return JsonMessage.getString(500, "解析失败");
        }
        try {
            FileUtil.writeString(context, file, CharsetUtil.UTF_8);
        } catch (Exception e) {
            DefaultSystemLog.getLog().error(e.getMessage(), e);
            return JsonMessage.getString(400, "操作失败:" + e.getMessage());
        }
        String msg = this.reloadNginx();
        return JsonMessage.getString(200, "提交成功" + msg);
    }

    private String reloadNginx() {
        String serviceName = nginxService.getServiceName();
        try {
            String format = StrUtil.format("{} -s reload", serviceName);
            String msg = CommandUtil.execSystemCommand(format);
            if (StrUtil.isNotEmpty(msg)) {
                DefaultSystemLog.getLog().info(msg);
                return "(" + msg + ")";
            }
        } catch (Exception e) {
            DefaultSystemLog.getLog().error("reload nginx error", e);
        }
        return StrUtil.EMPTY;
    }

    /**
     * 权限检查 防止非系统管理员配置静态资源访问
     *
     * @param ngxBlock 代码片段
     * @return false 不正确
     */
    private boolean checkRootRole(NgxBlock ngxBlock) {
//        UserModel userModel = getUser();
        //        List<NgxEntry> locationAll = ngxBlock.findAll(NgxBlock.class, "location");
        //        if (locationAll != null) {
        //            for (NgxEntry ngxEntry1 : locationAll) {
        //                NgxBlock ngxBlock1 = (NgxBlock) ngxEntry1;
        //                NgxParam locationMain = ngxBlock1.findParam("root");
        //                if (locationMain == null) {
        //                    locationMain = ngxBlock1.findParam("alias");
        //                }
        //
        //            }
        //        }
        return true;
    }

    /**
     * 删除配置
     *
     * @param path 文件路径
     * @param name 文件名
     * @return json
     */
    @RequestMapping(value = "delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String delete(String path, String name) {
        if (!whitelistDirectoryService.checkNgxDirectory(path)) {
            return JsonMessage.getString(400, "非法操作");
        }
        String safePath = pathSafe(path);
        String safeName = pathSafe(name);
        if (StrUtil.isEmpty(safeName)) {
            return JsonMessage.getString(400, "删除失败,请正常操作");
        }
        File file = FileUtil.file(safePath, safeName);
        try {
            FileUtil.rename(file, file.getName() + "_back", false, true);
        } catch (Exception e) {
            DefaultSystemLog.getLog().error("删除nginx", e);
            return JsonMessage.getString(400, "删除失败:" + e.getMessage());
        }
        String msg = this.reloadNginx();
        return JsonMessage.getString(200, "删除成功" + msg);
    }

    /**
     * 获取nginx状态
     *
     * @return json
     */
    @RequestMapping(value = "status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String status() {
        String name = nginxService.getServiceName();
        if (StrUtil.isEmpty(name)) {
            return JsonMessage.getString(500, "服务名错误");
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        boolean serviceStatus = AbstractSystemCommander.getInstance().getServiceStatus(name);
        jsonObject.put("status", serviceStatus);
        return JsonMessage.getString(200, "", jsonObject);
    }

    /**
     * 修改nginx配置
     *
     * @param name 服务名
     * @return json
     */
    @RequestMapping(value = "updateConf", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String updateConf(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "服务名称错误") String name) {
        JSONObject ngxConf = nginxService.getNgxConf();
        ngxConf.put("name", name);
        nginxService.save(ngxConf);
        return JsonMessage.getString(200, "修改成功");
    }

    /**
     * 获取配置信息
     *
     * @return json
     */
    @RequestMapping(value = "config", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String config() {
        JSONObject ngxConf = nginxService.getNgxConf();
        return JsonMessage.getString(200, "", ngxConf);
    }

    /**
     * 启动nginx
     *
     * @return json
     */
    @RequestMapping(value = "open", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String open() {
        String name = nginxService.getServiceName();
        String result = AbstractSystemCommander.getInstance().startService(name);
        return JsonMessage.getString(200, "nginx服务已启动 " + result);
    }

    /**
     * 关闭nginx
     *
     * @return json
     */
    @RequestMapping(value = "close", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String close() {
        String name = nginxService.getServiceName();
        String result = AbstractSystemCommander.getInstance().stopService(name);
        return JsonMessage.getString(200, result);
    }

    /**
     * 重新加载
     *
     * @return json
     */
    @RequestMapping(value = "reload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String reload() {
        String name = nginxService.getServiceName();
        if (SystemUtil.getOsInfo().isLinux()) {
            String result = CommandUtil.execSystemCommand(StrUtil.format("{} -t", name));
            if (StrUtil.isNotEmpty(result)) {
                return JsonMessage.getString(400, result);
            }
        } else if (SystemUtil.getOsInfo().isWindows()) {
            String result = CommandUtil.execSystemCommand("sc qc " + name);
            List<String> strings = StrSpliter.splitTrim(result, "\n", true);
            //服务路径
            File file = null;
            for (String str : strings) {
                str = str.toUpperCase().trim();
                if (str.startsWith("BINARY_PATH_NAME")) {
                    String path = str.substring(str.indexOf(":") + 1).replace("\"", "").trim();
                    file = FileUtil.file(path).getParentFile();
                    break;
                }
            }
            result = CommandUtil.execSystemCommand("nginx -t", file);
            if (StrUtil.isNotEmpty(result)) {
                return JsonMessage.getString(400, result);
            }
        }
        this.reloadNginx();
        return JsonMessage.getString(200, "重新加载成功");
    }
}