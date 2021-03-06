package gzx.devops.controller.build;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import gzx.devops.common.BaseServerController;
import gzx.devops.common.ServerOpenApi;
import gzx.devops.model.data.BuildModel;
import gzx.devops.plugin.ClassFeature;
import gzx.devops.plugin.Feature;
import gzx.devops.plugin.MethodFeature;
import gzx.devops.service.build.BuildService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * 触发器
 */
@Controller
@RequestMapping(value = "/build")
@Feature(cls = ClassFeature.BUILD)
public class BuildTriggerController extends BaseServerController {

    @Resource
    private BuildService buildService;

    @RequestMapping(value = "trigger.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public String trigger(String id) {
        BuildModel item = buildService.getItem(id);
        //
        if (StrUtil.isEmpty(item.getTriggerToken())) {
            item.setTriggerToken(RandomUtil.randomString(10));
            buildService.updateItem(item);
        }
        setAttribute("item", item);
        //
        String contextPath = getRequest().getContextPath();
        String url = ServerOpenApi.BUILD_TRIGGER_BUILD.
                replace("{id}", item.getId()).
                replace("{token}", item.getTriggerToken());
        String triggerBuildUrl = String.format("/%s/%s", contextPath, url);
        setAttribute("triggerBuildUrl", FileUtil.normalize(triggerBuildUrl));
        return "build/trigger";
    }


    @RequestMapping(value = "trigger_rest.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.EDIT)
    public String triggerRest(String id) {
        BuildModel item = buildService.getItem(id);
        //
        item.setTriggerToken(RandomUtil.randomString(10));
        buildService.updateItem(item);
        return JsonMessage.getString(200, "ok");
    }
}
