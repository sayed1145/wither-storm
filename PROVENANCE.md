# 来源与许可范围

## 本次实际下载的头模型

- 作者 / 仓库：**TheCheesyChip / Witherbean**，`witherbeans-wither-storm-mod-elder`。
- 仓库：https://github.com/TheCheesyChip/witherbeans-wither-storm-mod-elder
- 固定提交：`4db69ee473d88aa28aeb3d7dc6aed315d90e0d03`。
- 原模型：[WitherStormHeadModel.java](https://github.com/TheCheesyChip/witherbeans-wither-storm-mod-elder/blob/4db69ee473d88aa28aeb3d7dc6aed315d90e0d03/src/main/java/net/witherbean/wwsm/client/model/wither_storm/head/WitherStormHeadModel.java)
- 原贴图：[wither_storm_head.png](https://github.com/TheCheesyChip/witherbeans-wither-storm-mod-elder/blob/4db69ee473d88aa28aeb3d7dc6aed315d90e0d03/src/main/resources/assets/wwsm/textures/entity/wither_storm/head/wither_storm_head.png)
- 原模型 SHA-256：`7842ad246ddfdac5d2d60cdaa8854cbae3ba9a0d435e81b5dff648055050507b`。已与固定提交的网络文件逐字节核对。
- 仓库原始 MIT 许可保存在 `third_party/witherbean/LICENSE`；JAR 内也含 `licenses/witherbean-MIT.txt`。保留原版权行 `Copyright (c) 2024`，不擅自补写版权人。

这是从该仓库实际下载的 Java 方块模型和 PNG，不是从截图手工描摹，也不是从 Sketchfab 浏览器缓存抓取。此前需要登录或被拒绝的模型下载没有作为本版资产来源。

### 转换与改动

`tools/import_witherbean.py` 提取 top / jaw 的 texOffs、addBox 和 PartPose.offset；排除原模型的零厚度紫光平面。46 个立方体对应原数据。坐标轴转换为本引擎的 Z-up，并使用统一 2.4 倍模型尺度；运行时另外叠加全身成长与轻微下颌位移。贴图使用原 PNG 的六面 UV 区域，没有用程序纹理冒充下载贴图。输出的 `.bbmodel` 是本次转换产品，不是作者发布的原文件；原动画文件作为来源资料保留，未宣称整套 Minecraft 动画移植成功。

## 继承部分

三维投影器、骨骼基础、普通头颅、命令核心、部分逻辑与音频来自用户提供的工程及其后续修订。程序化躯体、牵引与建筑独立吸收、环绕方块是在此基础上修改。不能把整份工程或继承音乐声称为本次原创，也不能因一个资产采用 MIT 就把整个工程称为 MIT。

Minecraft、Minecraft Story Mode、Mindustry 等名称及原作品权利属于各自权利方。上述 MIT 是来源仓库给出的许可，不代表商标授权或对所有上游权利的独立法律审查。原附件音乐的再分发许可未重新核实，对外发布前仍应确认。

## v1.4.0 补充

本次继续使用同一个下载头及原 PNG。新增的 StormAnimation 运行时动作曲线、头部状态联动和 DepthSurface 深度渲染代码为本次工程修改，不是冒称移植了作者原来的 Minecraft 动画时间轴。原 `.bbmodel` 的几何转换性质未变。

## v1.6.0 材质与图腾

身体与环绕方块使用用户本轮上传的 `程序化材质图集.png`，原件按字节保存为 `assets/textures/supplied-material-atlas.png`，SHA-256 为 `80509f111ffd5fb6e58d37f061a21f06a66998cb4086321c663b120053f9790d`。第二排最后两个切片从身体图集映射中排除；原命令方块材质独立生成。未为用户提供的图集擅自指定 MIT 许可。图腾图标和绘制程序是本轮新增的三首像素造型，不声称下载自某个外部图腾模型。

## v1.7.0 朝向、发射孔和攻击时间轴

继续使用原已下载的 46 方块头与 PNG，没有新增外来头模型。移除本工程生成的颈部，调整嵌入身体的安装点；成熟头发射点为其上半头子骨骼内 `(0, 16.82, 0)`，即紫色孔前表面外侧，随该子骨骼缩放和姿态变换。普通头沿自身眼面发射。身体/头部独立朝向、触手蓄力挥击时间轴和敌我聚合界面是本轮工程修改，不宣称移植了原 Minecraft 游戏代码或官方动画。

## v1.7.1
没有新增外部模型。新增共享深度透明光束通道、实际网格/像素对照测试、原生HUD边界避让及可保存的面板拖动。诊断截图临时使用绿色仅为测量，不是生产配色修改。
