package com.distance.third.dji.log;

/**
 * 日志树管理接口
 *
 * @author sky on 2018/2/26
 */
public interface LogTreeManager {

    boolean addLogTree(LogTree logTree);

    boolean addLogTrees(LogTree... logTrees);

    boolean removeLogTree(LogTree logTree);

    boolean clearTrees();
}
