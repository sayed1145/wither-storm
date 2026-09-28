# 历史存档测试夹具

这三个存档由已交付的 v1.5.0 JAR 实际运行 `tools/run_checks.sh headless` 生成，并非用 v1.6.0 伪造旧版序列化。

原始 v1.5.0 JAR SHA-256：68e9a6c360f9cf8a0b0d0eeba75b7ac199bf3b830b01d7b0368b17aaf3213bdb

- storm-check.msav：普通成长与核心状态。
- v15-passive.msav：非光束生物吸收中的单位链接。
- inflight-building.msav：旧版单个建筑尚在飞行中，剩余95 tick。

LegacyChecks 使用当前安装 JAR 读取它们，检查玩家接管能力、新容量、实体链接、旧飞行迁移及只计数一次。
