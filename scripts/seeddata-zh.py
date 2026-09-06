# -*- coding: utf-8 -*-
"""SeedData.java 演示数据中文化(简单字符串字面量替换)。"""
import io, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

PATH = r'D:\Homework\软件开发实践2\software-development\src\main\java\hdu\ljq\service\SeedData.java'
src = open(PATH, encoding='utf-8').read()

PAIRS = [
    # 产品名
    ('"Mini Bowling Play Set"', '"迷你保龄球套装"'),
    ('"Balance & Move Kit"', '"平衡与移动套装"'),
    ('"Play Anywhere Ball Set"', '"随处玩乐球类套装"'),
    ('"Family Bowling Challenge"', '"家庭保龄球挑战套装"'),
    ('"Jump & Discover Kit"', '"跳跃探索套装"'),
    ('"Outdoor Adventure Set"', '"户外冒险套装"'),
    # 图片 alt
    ('"Six wooden bowling pins with colored bands and a small blue ball"', '"彩色木制保龄球瓶与一颗蓝色小球"'),
    ('"Five stepping stones and a wooden balance beam in a bright playroom"', '"五个踏脚石与一根木质平衡木,置于明亮的游戏室"'),
    ('"Three soft fabric play balls, marker cones and a canvas carry bag"', '"三颗布质软球、标志锥和一个帆布收纳袋"'),
    ('"A family playing with a ten-pin wooden bowling challenge set"', '"一家人正在玩十瓶木质保龄球挑战套装"'),
    ('"A child using floor spots, soft hurdles and a cotton jump rope"', '"孩子在使用地面圆点、软栏和棉质跳绳"'),
    ('"Children playing with a portable ball, beanbag, cone and ring-toss set outdoors"', '"孩子们在户外玩便携球、豆袋、锥桶与套圈套装"'),
    # 短描述
    ('"Six smooth wooden pins, one little blue ball and a very satisfying strike."', '"六根光滑木制球瓶、一颗蓝色小球,带来一次非常满足的全中。"'),
    ('"Step, balance and build a new path with five stones and a wooden bridge."', '"踩着五块石头和木质平衡木,迈步、保持平衡,走出属于你们的新路线。"'),
    ('"Three soft balls, four marker cones and one carry bag for play wherever you go."', '"三颗软球、四个标志锥和一个收纳袋,走到哪里都能玩起来。"'),
    ('"Ten wooden pins, two balls and score tiles turn the living room into a family lane."', '"十根木质球瓶、两颗球和记分牌,把客厅变成家庭球道。"'),
    ('"A jump rope, six activity spots and four soft hurdles for a course that changes every day."', '"一条跳绳、六个活动圆点和四个软栏,让赛道每天都能变化。"'),
    ('"Balls, beanbags, cones and ring toss come together in one portable backyard challenge."', '"球、豆袋、锥桶和套圈组合成一套便携的后院挑战。"'),
    # 组件
    ('"6 beechwood pins · 1 wooden ball"', '"6 根山毛榉球瓶 · 1 颗木球"'),
    ('"5 non-slip stepping stones · 1 wooden balance beam"', '"5 个防滑踏脚石 · 1 根木质平衡木"'),
    ('"3 soft fabric balls · 4 marker cones · 1 canvas bag"', '"3 颗布质软球 · 4 个标志锥 · 1 个帆布袋"'),
    ('"10 beechwood pins · 2 wooden balls · score tiles"', '"10 根山毛榉球瓶 · 2 颗木球 · 记分牌"'),
    ('"1 cotton jump rope · 6 floor spots · 4 soft hurdles"', '"1 条棉质跳绳 · 6 个地面圆点 · 4 个软栏"'),
    ('"2 soft balls · 6 beanbags · 4 cones · 3 ring-toss pegs · carry tote"', '"2 颗软球 · 6 个豆袋 · 4 个锥桶 · 3 个套圈桩 · 收纳提袋"'),
    # 分类
    ('"Bowling & aim"', '"保龄球与瞄准"'),
    ('"Balance & coordination"', '"平衡与协调"'),
    ('"Outdoor games"', '"户外游戏"'),
    ('"Simple ways to discover the joy of movement."', '"用简单的方式发现运动的乐趣。"'),
    # 站点
    ('"WEMOVE SPORTS"', '"WEMOVE"'),
    ('"Small moves. Big discoveries."', '"小小的动作,大大的发现。"'),
    # hero
    ('"Small moves.\\nBig discoveries."', '"小小的动作\\n大大的发现。"'),
    ('"More movement. More imagination. More moments together. Discover a world of"\n                      + " active play for the whole family."',
     '"更多的运动、更多的想象、更多的亲子时刻。一起发现属于全家的运动世界。"'),
    ('"A family playing together with the WEMOVE active-play collection"', '"一家人正在玩 WEMOVE 运动玩乐系列"'),
    ('"Find your next adventure"', '"开启下一场冒险"'),
    # dealer cta
    ('"Let\'s get more people moving."', '"让更多人一起动起来。"'),
    ('"Bring the joy of active play to your community. Start a conversation about"\n                      + " becoming a WEMOVE partner."',
     '"把快乐运动的喜悦带到你的社区。与我们聊聊,成为 WEMOVE 合作伙伴。"'),
    ('"Become a partner"', '"成为合作伙伴"'),
    # seo 后缀
    ('title + " | WEMOVE SPORTS"', 'title + " | WEMOVE"'),
    # 产品演示文案
    ('"## Everything in the picture\\n"', '"## 图中的每一件都在这里\\n"'),
    ('"This sample set includes "', '"本示例套装包含 "'),
    ('". Every item named here is represented in the product image.\\n\\n"', '"。图片中出现的每一件物品都包含在内。\\n\\n"'),
    ('"## Make the challenge your own\\n"', '"## 让挑战成为你的专属\\n"'),
    ('"Start with one simple game, then rearrange the pieces, change the distance or invite"\n            + " another player. The open-ended format makes it easy to create a fresh activity for"\n            + " different spaces and confidence levels.\\n\\n"',
     '"从一个简单的小游戏开始,然后重新摆放道具、改变距离或邀请更多玩家。开放式设计让您轻松为不同空间和不同水平的孩子创造新玩法。\\n\\n"'),
    ('"## Sample catalog information\\n"', '"## 示例目录说明\\n"'),
    ('"This is a product concept for demonstration. Confirm final materials, dimensions and"\n            + " safety instructions before commercial use."',
     '"此为演示用产品概念。商用前请确认最终材质、尺寸与安全说明。"'),
    ('"The complete set is shown in the product image"', '"完整套装内容见产品图片"'),
    ('"Rearrange the pieces to create new challenges"', '"重新组合道具,创造新挑战"'),
    ('"Designed for movement, imagination and shared play"', '"为运动、想象与亲子共玩而设计"'),
    ('"Set includes"', '"套装包含"'),
    ('"Suggested play"', '"建议玩法"'),
    ('"Catalog status"', '"目录状态"'),
    ('"Demonstration product concept"', '"演示产品概念"'),
    ('"Indoor play"', '"室内玩法"'),
    ('"Outdoor play"', '"户外玩法"'),
    ('"Indoor or outdoor play"', '"室内或户外玩法"'),
]

count = 0
missed = []
for old, new in PAIRS:
    if old in src:
        src = src.replace(old, new)
        count += 1
    else:
        missed.append(old[:60])

open(PATH, 'w', encoding='utf-8', newline='').write(src)
print(f'替换成功: {count}/{len(PAIRS)}')
if missed:
    print('未匹配:')
    for m in missed:
        print('  ', repr(m))
