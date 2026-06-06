$replacements = @(
    @{Pattern = 'client\.textRenderer'; Replacement = 'client.font'},
    @{Pattern = 'client\.currentScreen'; Replacement = 'client.screen'},
    @{Pattern = '\.getScaledWidth\(\)'; Replacement = '.getGuiScaledWidth()'},
    @{Pattern = '\.getScaledHeight\(\)'; Replacement = '.getGuiScaledHeight()'},
    @{Pattern = 'MinecraftClient\.getInstance'; Replacement = 'Minecraft.getInstance'},
    @{Pattern = 'import net\.minecraft\.util\.Formatting;'; Replacement = 'import net.minecraft.ChatFormatting;'},
    @{Pattern = '(?<![a-zA-Z])Formatting\.'; Replacement = 'ChatFormatting.'},
    @{Pattern = 'client\.inGameHud'; Replacement = 'client.gui'},
    @{Pattern = 'Util\.getMeasuringTimeMs'; Replacement = 'Util.getMillis'},
    @{Pattern = '\.gui\.getTicks\(\)'; Replacement = '.gui.getGuiTicks()'},
    @{Pattern = '\.fontHeight'; Replacement = '.lineHeight'},
    @{Pattern = 'client\.mouse\.getX\(\)'; Replacement = 'client.mouseHandler.xpos()'},
    @{Pattern = 'client\.mouse\.getY\(\)'; Replacement = 'client.mouseHandler.ypos()'},
    @{Pattern = '\.drawCenteredString\('; Replacement = '.centeredText('},
    @{Pattern = 'import net\.minecraft\.client\.sound\.PositionedSoundInstance;'; Replacement = 'import net.minecraft.client.resources.sounds.SimpleSoundInstance;'},
    @{Pattern = 'PositionedSoundInstance\.master'; Replacement = 'SimpleSoundInstance.forUI'},
    @{Pattern = 'import io\.github\.darkkronicle\.advancedchatcore\.interfaces\.IMessageProcessor\.Text;'; Replacement = 'import net.minecraft.network.chat.Component;'},
    @{Pattern = '\bText\b(?=\s)'; Replacement = 'Component'},

    @{Pattern = '\.width\(\)'; Replacement = '.getWidth()'},
    @{Pattern = '\.asOrderedText\(\)'; Replacement = ''},
    @{Pattern = 'Minecraft\.getInstance\(\)\.textRenderer'; Replacement = 'Minecraft.getInstance().font'},
    @{Pattern = '\.withFormatting\('; Replacement = '.withColor('},
    @{Pattern = 'MinecraftClient mc'; Replacement = 'Minecraft mc'},
    @{Pattern = 'import net\.minecraft\.client\.MinecraftClient;'; Replacement = 'import net.minecraft.client.Minecraft;'},
    @{Pattern = 'mc\.textRenderer'; Replacement = 'mc.font'},
    @{Pattern = 'Text\.literal\('; Replacement = 'Component.literal('},
    @{Pattern = '\.keyboard\.setClipboard\('; Replacement = '.keyboardHandler.setClipboard('},
    @{Pattern = 'net\.minecraft\.item\.ItemStack'; Replacement = 'net.minecraft.world.item.ItemStack'},
    @{Pattern = '\.getAction\(\)'; Replacement = '.action()'},
    @{Pattern = '\.runDirectory'; Replacement = '.gameDirectory'},
    @{Pattern = 'Util\.getOperatingSystem\(\)\.open\('; Replacement = 'Util.getPlatform().openUri('},
    @{Pattern = '\.networkHandler'; Replacement = '.connection'},
    @{Pattern = 'import net\.minecraft\.util\.math\.MathHelper;'; Replacement = 'import net.minecraft.util.Mth;'},
    @{Pattern = 'MathHelper\.'; Replacement = 'Mth.'},
    @{Pattern = 'import net\.minecraft\.util\.OrderedText;'; Replacement = 'import net.minecraft.util.FormattedCharSequence;'},
    @{Pattern = '\bOrderedText\b'; Replacement = 'FormattedCharSequence'},
    @{Pattern = 'ChatMessages\.'; Replacement = 'ComponentRenderUtils.'}
)

Get-ChildItem -Path "src" -Filter "*.java" -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $originalContent = $content
    
    foreach ($r in $replacements) {
        $content = $content -replace $r.Pattern, $r.Replacement
    }
    
    if ($content -ne $originalContent) {
        Set-Content -Path $_.FullName -Value $content -NoNewline
        Write-Host "Updated: $($_.FullName)"
    }
}

Write-Host "Done!"

