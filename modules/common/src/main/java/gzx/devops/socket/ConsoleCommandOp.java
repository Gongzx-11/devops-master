package gzx.devops.socket;

/**
 * 控制台socket 操作枚举

 */
public enum ConsoleCommandOp {
    /**
     * 启动
     */
    start,
    stop,
    restart,
    status,
    /**
     * 运行日志
     */
    showlog,
    /**
     * 查看内存信息
     */
    top,
    /**
     * 心跳
     */
    heart
}
