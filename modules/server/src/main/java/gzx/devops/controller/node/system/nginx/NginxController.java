package gzx.devops.controller.node.system.nginx;

import com.alibaba.fastjson.JSONObject;
import gzx.devops.common.BaseServerController;
import gzx.devops.common.forward.NodeForward;
import gzx.devops.common.forward.NodeUrl;
import gzx.devops.common.interceptor.OptLog;
import gzx.devops.model.log.UserOperateLogV1;
import gzx.devops.plugin.ClassFeature;
import gzx.devops.plugin.Feature;
import gzx.devops.plugin.MethodFeature;
import gzx.devops.service.system.WhitelistDirectoryService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * nginx 管理
 */
@Controller
@RequestMapping("/node/system/nginx")
@Feature(cls = ClassFeature.NGINX)
public class NginxController extends BaseServerController {

    @Resource
    private WhitelistDirectoryService whitelistDirectoryService;


    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @Feature(method = MethodFeature.LIST)
    public String ngx() {
        return "node/system/nginx";
    }

    /**
     * 配置列表
     *
     * @return json
     */
    @RequestMapping(value = "list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.LIST)
    public String list() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_list_data).toString();
    }

    /**
     * 配置列表
     *
     * @return json
     */
    @RequestMapping(value = "tree.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.LIST)
    public String tree() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_Tree).toString();
    }


    @RequestMapping(value = "item.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public String setting(String type) {
        List<String> ngxDirectory = whitelistDirectoryService.getNgxDirectory(getNode());
        setAttribute("nginx", ngxDirectory);
        setAttribute("type", type);
        JSONObject data = NodeForward.requestData(getNode(), NodeUrl.System_Nginx_item_data, getRequest(), JSONObject.class);
        setAttribute("data", data);
        return "node/system/nginxSetting";
    }


    @RequestMapping(value = "updateNgx", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.SaveNginx)
    @Feature(method = MethodFeature.EDIT)
    public String updateNgx() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_updateNgx).toString();
    }


    @RequestMapping(value = "delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.DelNginx)
    @Feature(method = MethodFeature.DEL)
    public String delete() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_delete).toString();
    }

    /**
     * 获取nginx状态
     *
     * @return json
     */
    @RequestMapping(value = "status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.LIST)
    public String status() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_status).toString();
    }

    /**
     * 获取nginx配置状态
     *
     * @return json
     */
    @RequestMapping(value = "config", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.CONFIG)
    public String config() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_config).toString();
    }

    /**
     * 启动nginx
     *
     * @return json
     */
    @RequestMapping(value = "open", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.EXECUTE)
    public String open() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_open).toString();
    }

    /**
     * 关闭nginx
     *
     * @return json
     */
    @RequestMapping(value = "close", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.EXECUTE)
    public String close() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_close).toString();
    }


    /**
     * 修改nginx
     *
     * @return json
     */
    @RequestMapping(value = "updateConf", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.CONFIG)
    public String updateConf() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_updateConf).toString();
    }

    @RequestMapping(value = "reload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.EXECUTE)
    public String reload() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_reload).toString();
    }

}
