package gzx.devops.service.manage;

import gzx.devops.common.commander.AbstractProjectCommander;
import gzx.devops.model.data.ProjectInfoModel;
import gzx.devops.socket.ConsoleCommandOp;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 控制台
 */
@Service
public class ConsoleService {
    @Resource
    private ProjectInfoService projectInfoService;

    /**
     * 执行shell命令
     * @ gongzaixing
     * @param consoleCommandOp 执行的操作
     * @param projectInfoModel 项目信息
     * @return 执行结果
     * @throws Exception 异常
     */
    public String execCommand(ConsoleCommandOp consoleCommandOp, ProjectInfoModel projectInfoModel) throws Exception {
        String result;
        AbstractProjectCommander abstractProjectCommander = AbstractProjectCommander.getInstance();
        // 执行命令
        switch (consoleCommandOp) {
            case restart:
                result = abstractProjectCommander.restart(projectInfoModel);
                break;
            case start:
                result = abstractProjectCommander.start(projectInfoModel);
                break;
            case stop:
                result = abstractProjectCommander.stop(projectInfoModel);
                break;
            case status: {
                String tag = projectInfoModel.getId();
                result = abstractProjectCommander.status(tag);
                break;
            }
            case top:
            case showlog:
            default:
                throw new IllegalArgumentException(consoleCommandOp + " error");
        }
        //  通知日志刷新
        if (consoleCommandOp == ConsoleCommandOp.start || consoleCommandOp == ConsoleCommandOp.restart) {
            // 修改 run lib 使用情况
            ProjectInfoModel modify = projectInfoService.getItem(projectInfoModel.getId());
            modify.setRunLibDesc(projectInfoModel.getUseLibDesc());
            try {
                projectInfoService.updateItem(modify);
            } catch (Exception ignored) {
            }
        }
        return result;
    }
}
