package com.resourcetoggle.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ResourceToggleConfigScreen extends Screen {
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(
            "textures/gui/options_background.png");
    private final Screen lastScreen;
    private List<Pack> availablePacks;
    private List<String> selectedPacks;
    private EditBox searchBox;
    private String searchQuery = "";
    private int currentPage = 0;
    private static final int ENTRIES_PER_PAGE = 6;
    private Button previousButton;
    private Button nextButton;
    private int scrollOffset = 0;
    private boolean isDragging = false;
    private int dragStartY;
    private int initialScrollOffset;

    // UI Constants
    private static final int HEADER_HEIGHT = 60;
    private static final int FOOTER_HEIGHT = 40;
    private static final int ENTRY_HEIGHT = 40;
    private static final int LIST_PADDING = 8;

    // Colors
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int SUBTITLE_COLOR = 0xCCCCCC;
    private static final int SELECTED_COLOR = 0x55FF55;
    private static final int SEARCH_COLOR = 0xAAAAAA;
    private static final int DESCRIPTION_COLOR = 0xCCCCCC;
    private static final int BACKGROUND_TOP = 0xC0101010;
    private static final int BACKGROUND_BOTTOM = 0xD0202020;
    private static final int SCROLLBAR_COLOR = 0x99FFFFFF;
    private static final int SCROLLBAR_HOVER_COLOR = 0xBBFFFFFF;

    public ResourceToggleConfigScreen(Screen lastScreen) {
        super(Component.literal("Resource Pack Toggle"));
        this.lastScreen = lastScreen;
        this.selectedPacks = new ArrayList<>(ModConfig.get().getSelectedPacks());
    }

    @Override
    protected void init() {
        // Initialize search box with improved styling
        searchBox = new EditBox(this.font, width / 2 - 120, 32, 240, 20, Component.literal("Search..."));
        searchBox.setMaxLength(50);
        searchBox.setValue(searchQuery);
        searchBox.setResponder(this::updateSearch);
        searchBox.setTooltip(Tooltip.create(Component.literal("Search for resource packs by name")));
        addRenderableWidget(searchBox);

        // Get and filter available packs
        try {
            availablePacks = new ArrayList<>(Minecraft.getInstance().getResourcePackRepository().getAvailablePacks());
            if (searchQuery != null && !searchQuery.isEmpty()) {
                availablePacks = availablePacks.stream()
                        .filter(pack -> pack.getId().toLowerCase().contains(searchQuery.toLowerCase()) ||
                                pack.getDescription().getString().toLowerCase().contains(searchQuery.toLowerCase()))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            availablePacks = new ArrayList<>();
        }

        // Add pagination buttons with improved appearance
        int totalPages = Math.max(1, (int) Math.ceil(availablePacks.size() / (double) ENTRIES_PER_PAGE));

        previousButton = Button.builder(Component.literal("◀"), button -> {
            if (currentPage > 0) {
                currentPage--;
                scrollOffset = 0;
                init();
            }
        })
                .pos(width / 2 - 50, height - 30) // Moved further left from center
                .size(20, 20)
                .build();
        previousButton.active = currentPage > 0;
        addRenderableWidget(previousButton);

        nextButton = Button.builder(Component.literal("▶"), button -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                scrollOffset = 0;
                init();
            }
        })
                .pos(width / 2 + 30, height - 30) // Moved further right from center
                .size(20, 20)
                .build();
        nextButton.active = currentPage < totalPages - 1;
        addRenderableWidget(nextButton);

        // Add done button with improved styling
        addRenderableWidget(
                Button.builder(Component.literal("Done"), button -> onClose())
                        .pos(width / 2 - 150, height - 30) // Moved further left
                        .size(70, 20)
                        .tooltip(Tooltip.create(Component.literal("Save and return")))
                        .build());

        // Add reset button
        addRenderableWidget(
                Button.builder(Component.literal("Reset"), button -> {
                    clearSelection();
                    init();
                })
                        .pos(width / 2 + 80, height - 30) // Moved further right
                        .size(70, 20)
                        .tooltip(Tooltip.create(Component.literal("Clear resource pack selection")))
                        .build());
    }

    private void updateSearch(String query) {
        currentPage = 0;
        scrollOffset = 0;
        searchQuery = query;
        init();
    }

    private void selectPack(String packId) {
        if (selectedPacks.contains(packId)) {
            selectedPacks.remove(packId);
        } else {
            if (selectedPacks.size() < 9) {
                selectedPacks.add(packId);
            } else {
                return; // Max 9 packs
            }
        }
        updateSelectedPacks();
        init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Render textured background
        renderBackgroundTexture(graphics);

        // Draw semi-transparent overlays for header and footer
        renderGradientBox(graphics, 0, 0, width, HEADER_HEIGHT, 0xE0000000, 0xB0000000);
        renderGradientBox(graphics, 0, height - FOOTER_HEIGHT, width, height, 0xB0000000, 0xE0000000);

        // Draw title with shadow
        graphics.drawCenteredString(font,
                Component.literal("Resource Pack Toggle").withStyle(style -> style.withBold(true)),
                width / 2, 12, TITLE_COLOR);

        // Draw search label
        graphics.drawString(font, Component.literal("Find packs:"), width / 2 - 120, 22, SEARCH_COLOR);

        // Draw current selection status with highlighting
        String status = selectedPacks.isEmpty() ? "Selected: None" : "Selected: " + selectedPacks.size() + " packs";
        renderHighlightedText(graphics, status, width / 2, height - 45, SELECTED_COLOR);

        // Draw pagination info with better formatting
        int totalPages = Math.max(1, (int) Math.ceil(availablePacks.size() / (double) ENTRIES_PER_PAGE));
        String pageInfo = String.format("%d of %d", currentPage + 1, totalPages);
        graphics.drawCenteredString(font, Component.literal(pageInfo), width / 2, height - 20, SUBTITLE_COLOR);

        // Setup the clipping region for the pack list
        int listTop = HEADER_HEIGHT + LIST_PADDING;
        int listBottom = height - FOOTER_HEIGHT - LIST_PADDING;
        int listHeight = listBottom - listTop;

        graphics.enableScissor(0, listTop, width, listBottom);

        // Render pack entries within the clipping region
        int contentHeight = renderPackEntries(graphics, listTop, listBottom, mouseX, mouseY);

        // Render scrollbar if needed
        graphics.disableScissor();
        renderScrollbar(graphics, listTop, listBottom, contentHeight, mouseX, mouseY);

        // Render tooltips and widgets
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderBackgroundTexture(GuiGraphics graphics) {
        // Apply darker background first
        graphics.fill(0, 0, width, height, 0xC0101010);

        // Render tile pattern with lower opacity
        RenderSystem.setShaderColor(0.15f, 0.15f, 0.15f, 1.0f);
        for (int x = 0; x <= width / 16; x++) {
            for (int y = 0; y <= height / 16; y++) {
                graphics.blit(BACKGROUND_TEXTURE, x * 16, y * 16, 0, 0, 16, 16, 16, 16);
            }
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // Enhance the top and bottom gradients
        renderGradientBox(graphics, 0, 0, width, HEADER_HEIGHT + 4, 0xD0101010, 0x00000000);
        renderGradientBox(graphics, 0, height - (FOOTER_HEIGHT + 4), width, height, 0x00000000, 0xD0101010);
    }

    private void renderGradientBox(GuiGraphics graphics, int x1, int y1, int x2, int y2, int colorTop,
            int colorBottom) {
        graphics.fillGradient(x1, y1, x2, y2, colorTop, colorBottom);
    }

    private void renderHighlightedText(GuiGraphics graphics, String text, int x, int y, int color) {
        int textWidth = font.width(text);
        graphics.fill(x - textWidth / 2 - 5, y - 2, x + textWidth / 2 + 5, y + 10, 0x44000000);
        graphics.drawCenteredString(font, text, x, y, color);
    }

    private int renderPackEntries(GuiGraphics graphics, int listTop, int listBottom, int mouseX, int mouseY) {
        int startIndex = currentPage * ENTRIES_PER_PAGE;
        int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, availablePacks.size());
        int totalEntries = endIndex - startIndex;
        int contentHeight = totalEntries * (ENTRY_HEIGHT + 4);

        // Clamp scroll offset
        int maxScroll = Math.max(0, contentHeight - (listBottom - listTop));
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

        // Render entries with scroll offset
        for (int i = startIndex; i < endIndex; i++) {
            Pack pack = availablePacks.get(i);
            int entryTop = listTop + (i - startIndex) * (ENTRY_HEIGHT + 4) - scrollOffset;

            // Only render visible entries
            if (entryTop + ENTRY_HEIGHT >= listTop && entryTop <= listBottom) {
                renderPackEntry(graphics, pack, entryTop, mouseX, mouseY);
            }
        }

        // If no packs are available, show a message
        if (availablePacks.isEmpty()) {
            String message = searchQuery.isEmpty() ? "No resource packs found"
                    : "No matching packs for \"" + searchQuery + "\"";
            graphics.drawCenteredString(font, message, width / 2, listTop + 30, DESCRIPTION_COLOR);
        }

        return contentHeight;
    }

    private void renderPackEntry(GuiGraphics graphics, Pack pack, int top, int mouseX, int mouseY) {
        int left = width / 2 - 180;
        int entryWidth = 360;
        String packId = pack.getId();
        boolean isSelected = selectedPacks.contains(packId);
        boolean isHovered = mouseX >= left && mouseX <= left + entryWidth &&
                mouseY >= top && mouseY <= top + ENTRY_HEIGHT;

        // Background with better styling
        int bgColorFrom = isSelected ? 0x90007700 : (isHovered ? 0x80444444 : 0x80222222);
        int bgColorTo = isSelected ? 0x90005500 : (isHovered ? 0x80333333 : 0x80111111);
        renderGradientBox(graphics, left, top, left + entryWidth, top + ENTRY_HEIGHT, bgColorFrom, bgColorTo);

        // Border
        int borderColor = isSelected ? 0xFF00AA00 : (isHovered ? 0x80FFFFFF : 0x40FFFFFF);
        graphics.fill(left, top, left + entryWidth, top + 1, borderColor);
        graphics.fill(left, top + ENTRY_HEIGHT - 1, left + entryWidth, top + ENTRY_HEIGHT, borderColor);
        graphics.fill(left, top, left + 1, top + ENTRY_HEIGHT, borderColor);
        graphics.fill(left + entryWidth - 1, top, left + entryWidth, top + ENTRY_HEIGHT, borderColor);

        // Pack name with truncation
        String name = pack.getId();
        int maxTitleWidth = entryWidth - 160;
        String truncatedName = truncateString(font, name, maxTitleWidth);

        int nameColor = isSelected ? SELECTED_COLOR : TITLE_COLOR;
        graphics.drawString(font, Component.literal(truncatedName).withStyle(style -> style.withBold(true)),
                left + 10, top + 8, nameColor);

        // Pack description with truncation
        String desc = pack.getDescription().getString();
        if (desc == null || desc.isEmpty())
            desc = "No description available";

        String truncatedDesc = truncateString(font, desc, maxTitleWidth);
        graphics.drawString(font, truncatedDesc, left + 10, top + 22, DESCRIPTION_COLOR);

        // Selection button
        int buttonWidth = 105;
        int buttonLeft = left + entryWidth - (buttonWidth + 10);
        int buttonTop = top + 10;
        int buttonHeight = 20;

        boolean isButtonHovered = mouseX >= buttonLeft && mouseX <= buttonLeft + buttonWidth &&
                mouseY >= buttonTop && mouseY <= buttonTop + buttonHeight;

        // Button styling
        int buttonColorTop = isSelected ? 0xFF007700 : (isButtonHovered ? 0xFF555555 : 0xFF333333);
        int buttonColorBottom = isSelected ? 0xFF005500 : (isButtonHovered ? 0xFF444444 : 0xFF222222);
        renderGradientBox(graphics, buttonLeft, buttonTop, buttonLeft + buttonWidth, buttonTop + buttonHeight,
                buttonColorTop, buttonColorBottom);

        // Button border and text
        int buttonBorderColor = isSelected ? 0xFF00FF00 : (isButtonHovered ? 0xFFAAAAAA : 0xFF666666);
        graphics.fill(buttonLeft, buttonTop, buttonLeft + buttonWidth, buttonTop + 1, buttonBorderColor);
        graphics.fill(buttonLeft, buttonTop + buttonHeight - 1, buttonLeft + buttonWidth, buttonTop + buttonHeight,
                buttonBorderColor);
        graphics.fill(buttonLeft, buttonTop, buttonLeft + 1, buttonTop + buttonHeight, buttonBorderColor);
        graphics.fill(buttonLeft + buttonWidth - 1, buttonTop, buttonLeft + buttonWidth, buttonTop + buttonHeight,
                buttonBorderColor);

        int packIndex = selectedPacks.indexOf(packId);
        Component buttonText = isSelected ? Component.literal("✓ Numpad " + (packIndex + 1)) : Component.literal("Select");
        int textWidth = font.width(buttonText);
        graphics.drawCenteredString(font, buttonText, buttonLeft + buttonWidth / 2, buttonTop + 6, 0xFFFFFF);
    }

    private String truncateString(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);

        // Binary search for the best fit
        int left = 0;
        int right = text.length();

        while (left < right) {
            int mid = (left + right + 1) / 2;
            String sub = text.substring(0, mid) + ellipsis;

            if (font.width(sub) <= maxWidth) {
                left = mid;
            } else {
                right = mid - 1;
            }
        }

        return text.substring(0, left) + ellipsis;
    }

    private void renderScrollbar(GuiGraphics graphics, int top, int bottom, int contentHeight, int mouseX, int mouseY) {
        int viewportHeight = bottom - top;

        // Only show scrollbar if content exceeds viewport
        if (contentHeight <= viewportHeight)
            return;

        int scrollbarRight = width / 2 + 160;
        int scrollbarLeft = scrollbarRight - 6;

        // Draw scrollbar track
        graphics.fill(scrollbarLeft, top, scrollbarRight, bottom, 0x40000000);

        // Calculate thumb size and position
        float contentRatio = (float) viewportHeight / contentHeight;
        int thumbHeight = Math.max(20, (int) (viewportHeight * contentRatio));
        int thumbRange = viewportHeight - thumbHeight;
        int maxScroll = contentHeight - viewportHeight;
        int thumbPosition = (maxScroll == 0) ? 0 : (int) (scrollOffset * thumbRange / maxScroll);

        // Draw thumb
        boolean thumbHovered = mouseX >= scrollbarLeft && mouseX <= scrollbarRight &&
                mouseY >= top + thumbPosition && mouseY <= top + thumbPosition + thumbHeight;
        int thumbColor = isDragging || thumbHovered ? SCROLLBAR_HOVER_COLOR : SCROLLBAR_COLOR;
        graphics.fill(scrollbarLeft, top + thumbPosition, scrollbarRight, top + thumbPosition + thumbHeight,
                thumbColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // Handle list entries clicking
        int listTop = HEADER_HEIGHT + LIST_PADDING;
        int listBottom = height - FOOTER_HEIGHT - LIST_PADDING;

        if (mouseY >= listTop && mouseY <= listBottom) {
            int startIndex = currentPage * ENTRIES_PER_PAGE;
            int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, availablePacks.size());

            for (int i = startIndex; i < endIndex; i++) {
                int entryTop = listTop + (i - startIndex) * (ENTRY_HEIGHT + 4) - scrollOffset;
                int entryBottom = entryTop + ENTRY_HEIGHT;

                if (mouseY >= entryTop && mouseY <= entryBottom) {
                    Pack pack = availablePacks.get(i);
                    int entryLeft = width / 2 - 150;
                    int entryWidth = 300;
                    int buttonLeft = entryLeft + entryWidth - 115;
                    int buttonWidth = 105;
                    int buttonTop = entryTop + 10;
                    int buttonBottom = buttonTop + 20;

                    // Check if button was clicked
                    if (mouseX >= buttonLeft && mouseX <= buttonLeft + buttonWidth &&
                            mouseY >= buttonTop && mouseY <= buttonBottom) {
                        selectPack(pack.getId());
                        return true;
                    }
                }
            }

            // Check for scrollbar drag start
            int scrollbarRight = width / 2 + 160;
            int scrollbarLeft = scrollbarRight - 6;
            int contentHeight = (endIndex - startIndex) * (ENTRY_HEIGHT + 4);

            // Only enable scrolling if content exceeds viewport
            if (contentHeight > (listBottom - listTop)) {
                // Calculate thumb position
                float contentRatio = (float) (listBottom - listTop) / contentHeight;
                int thumbHeight = Math.max(20, (int) ((listBottom - listTop) * contentRatio));
                int thumbRange = (listBottom - listTop) - thumbHeight;
                int maxScroll = contentHeight - (listBottom - listTop);
                int thumbPosition = (maxScroll == 0) ? 0 : (int) (scrollOffset * thumbRange / maxScroll);

                // Check if thumb was clicked
                if (mouseX >= scrollbarLeft && mouseX <= scrollbarRight &&
                        mouseY >= listTop + thumbPosition && mouseY <= listTop + thumbPosition + thumbHeight) {
                    isDragging = true;
                    dragStartY = (int) mouseY;
                    initialScrollOffset = scrollOffset;
                    return true;
                }
                // Check if track was clicked
                else if (mouseX >= scrollbarLeft && mouseX <= scrollbarRight) {
                    // Clicking above thumb scrolls up
                    if (mouseY < listTop + thumbPosition) {
                        scrollOffset = Math.max(0, scrollOffset - viewportHeight() / 2);
                        return true;
                    }
                    // Clicking below thumb scrolls down
                    else if (mouseY > listTop + thumbPosition + thumbHeight) {
                        int maxContentScroll = contentHeight - viewportHeight();
                        scrollOffset = Math.min(maxContentScroll, scrollOffset + viewportHeight() / 2);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging && button == 0) {
            int listTop = HEADER_HEIGHT + LIST_PADDING;
            int listBottom = height - FOOTER_HEIGHT - LIST_PADDING;
            int viewportHeight = listBottom - listTop;
            int contentHeight = getTotalContentHeight();

            if (contentHeight > viewportHeight) {
                int dragDistance = (int) mouseY - dragStartY;
                float dragRatio = (float) dragDistance / viewportHeight;
                int scrollAmount = (int) (dragRatio * contentHeight);

                scrollOffset = Mth.clamp(initialScrollOffset + scrollAmount, 0,
                        Math.max(0, contentHeight - viewportHeight));
                return true;
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDragging && button == 0) {
            isDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private int getTotalContentHeight() {
        return availablePacks.size() * (ENTRY_HEIGHT + 4);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        // Scroll the pack list
        scrollOffset = Math.max(0, scrollOffset - (int) (scrollAmount * 15));

        int viewportHeight = viewportHeight();
        int contentHeight = getTotalContentHeight();

        // Clamp scroll offset
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

        return true;
    }

    private int viewportHeight() {
        return height - FOOTER_HEIGHT - LIST_PADDING - (HEADER_HEIGHT + LIST_PADDING);
    }

    public void tick() {
        if (searchBox != null) {
            if (searchBox.isFocused()) {
                searchBox.setFocused(true);
            }
        }

        // Validate selected pack still exists
        if (!selectedPacks.isEmpty()) {
            boolean allPacksExist = selectedPacks.stream()
                    .allMatch(selectedId -> availablePacks.stream()
                            .anyMatch(pack -> pack.getId().equals(selectedId)));
            if (!allPacksExist) {
                // Just keep the ones that exist
                selectedPacks.removeIf(selectedId -> availablePacks.stream()
                        .noneMatch(pack -> pack.getId().equals(selectedId)));
                updateSelectedPacks();
            }
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(lastScreen);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String currentSearch = searchBox != null ? searchBox.getValue() : "";
        this.clearWidgets();
        super.resize(minecraft, width, height);
        this.searchQuery = currentSearch;
        init();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox.isFocused()) {
            return searchBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchBox.isFocused()) {
            return searchBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void initializeState() {
        this.selectedPacks = new ArrayList<>(ModConfig.get().getSelectedPacks());
    }

    private void clearSelection() {
        selectedPacks.clear();
        ModConfig.get().setSelectedPacks(new ArrayList<>());
        ModConfig.get().saveConfig();
        init();
    }

    private void updateSelectedPacks() {
        ModConfig.get().setSelectedPacks(new ArrayList<>(selectedPacks));
        ModConfig.get().saveConfig();
    }

    private void resetPack() {
        ModConfig.get().setSelectedPacks(new ArrayList<>());
        ModConfig.get().saveConfig();
    }
}