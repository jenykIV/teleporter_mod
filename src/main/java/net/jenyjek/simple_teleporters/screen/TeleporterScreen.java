package net.jenyjek.simple_teleporters.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.jenyjek.simple_teleporters.SimpleTeleporters;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class TeleporterScreen extends HandledScreen<TeleporterScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(SimpleTeleporters.MOD_ID, "textures/gui/teleporter_gui.png");

    public TeleporterScreen(TeleporterScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
    }
    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight);
        renderLapisBar(context, x, y);
        renderLock(context, x, y);
        tooltips(context,mouseX,mouseY,x,y);

    }

    private void tooltips(DrawContext context, int mouseX, int mouseY, int x, int y){
        if(isMouseOverRect(mouseX, mouseY, x, y, 156, 7, 7, 72)) setTooltip(Text.literal("lapis: " + handler.getLapisLeft()));
        if(!handler.getSlot(0).hasStack() && isMouseOverRect(mouseX, mouseY, x, y, 24, 32, 20, 20)) setTooltip(Text.literal("Insert cartridge to activate teleporter!"));
        if(handler.canTransferItems() == 16  && isMouseOverRect(mouseX, mouseY, x, y, 80, 48, 16, 16)) setTooltip(Text.literal("Requires upgrade!"));
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private boolean isMouseOverRect(int mouseX, int mouseY, int x, int y, int xpos, int ypos, int width, int height){
        return mouseX >= x+xpos && mouseX <= x+xpos + width && mouseY >= y+ypos && mouseY <= y+ypos + height;
    }

    private void renderLapisBar(DrawContext context, int x, int y){
        context.drawTexture(TEXTURE, x+156, y+7, 176, 0, 7, handler.powerLevel());
    }

    private void renderLock(DrawContext context, int x, int y){
        context.drawTexture(TEXTURE, x+80, y+48, 183, 0, 16, handler.canTransferItems());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    public void close() {
        super.close();
        SimpleTeleporters.LOGGER.info("Nigga");
    }
}
