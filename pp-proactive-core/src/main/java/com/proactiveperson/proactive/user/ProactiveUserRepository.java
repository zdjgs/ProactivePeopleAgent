package com.proactiveperson.proactive.user;

import java.util.List;

/**
 * 主动推送候选用户源（后续可换 DB / 后台配置）。
 */
public interface ProactiveUserRepository {

    List<ProactiveUser> findAllCandidates();
}
