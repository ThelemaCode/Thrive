import os
from PIL import Image

def convert_to_grayscale():
    current_dir = os.path.dirname(os.path.abspath(__file__))

    for filename in os.listdir(current_dir):
        if filename.lower().endswith(".png"):
            filepath = os.path.join(current_dir, filename)
            try:
                img = Image.open(filepath).convert("RGBA")

                # 1. 提取透明通道
                a = img.getchannel("A")

                # 2. 将 RGB 通道转为灰度 (L 模式会自动计算亮度)
                gray_img = img.convert("L")

                # 3. 合并灰度 RGB 与原透明通道
                result = Image.merge("RGBA", (gray_img, gray_img, gray_img, a))

                result.save(filepath, "PNG")
                print(f"已成功灰度化: {filename}")
            except Exception as e:
                print(f"处理失败: {filename}, {e}")

if __name__ == "__main__":
    convert_to_grayscale()