package gzx.devops.service.dblog;

import cn.hutool.db.Entity;
import gzx.devops.model.data.OutGivingNodeProject;
import gzx.devops.model.log.OutGivingLog;
import org.springframework.stereotype.Service;

/**
 * 分发日志
 */
@Service
public class DbOutGivingLogService extends BaseDbLogService<OutGivingLog> {

    public DbOutGivingLogService() {
        super(OutGivingLog.TABLE_NAME, OutGivingLog.class);
        setKey("id");
    }

    @Override
    public void insert(OutGivingLog outGivingLog) {
        outGivingLog.setStartTime(System.currentTimeMillis());
        if (outGivingLog.getStatus() == OutGivingNodeProject.Status.Cancel.getCode()) {
            outGivingLog.setEndTime(System.currentTimeMillis());
        }
        super.insert(outGivingLog);
    }

    @Override
    public int update(OutGivingLog outGivingLog) {
        Entity entity = new Entity();
        entity.set("status", outGivingLog.getStatus());
        // 结束
        entity.set("endTime", System.currentTimeMillis());
        entity.set("result", outGivingLog.getResult());
        //
        Entity where = new Entity();
        where.set("id", outGivingLog.getId());
        return super.update(entity, where);
    }
}
