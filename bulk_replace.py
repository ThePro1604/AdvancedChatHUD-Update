import os
import re

# Define the replacements
replacements = [
    (r'client\.textRenderer', 'client.font'),
    (r'client\.currentScreen', 'client.screen'),
    (r'\.getScaledWidth\(\)', '.getGuiScaledWidth()'),
    (r'\.getScaledHeight\(\)', '.getGuiScaledHeight()'),
    (r'MinecraftClient\.getInstance', 'Minecraft.getInstance'),
    (r'import net\.minecraft\.util\.Formatting;', 'import net.minecraft.ChatFormatting;'),
    (r'(?<![a-zA-Z])Formatting\.', 'ChatFormatting.'),
    (r'client\.inGameHud', 'client.gui'),
]

# Walk through the src directory
for root, dirs, files in os.walk('src'):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            
            original_content = content
            for pattern, replacement in replacements:
                content = re.sub(pattern, replacement, content)
            
            if content != original_content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f"Updated: {filepath}")

print("Done!")

