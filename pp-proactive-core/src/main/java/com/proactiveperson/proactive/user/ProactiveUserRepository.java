package com.proactiveperson.proactive.user;

import java.util.List;
import java.util.Optional;

/**
 * 主动推送候选用户源（后续可换 DB / 后台配置）。
 */
public interface ProactiveUserRepository {

    List<ProactiveUser> findAllCandidates();

    Optional<ProactiveUser> findByOpenId(String openId);

    Optional<ProactiveUser> findByUserId(String userId);
}
