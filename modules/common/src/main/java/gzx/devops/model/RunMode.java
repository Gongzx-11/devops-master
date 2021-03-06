package gzx.devops.model;

/**
 * 项目的运行方式
 */
public enum RunMode {
    /**
     * java -classpath
     */
    ClassPath,
    /**
     * java -jar
     */
    Jar,
    /**
     * java -jar  Springboot war
     */
    War,
    /**
     * 纯文件管理
     */
    File,
}
