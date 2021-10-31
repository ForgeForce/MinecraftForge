/*
 * Minecraft Forge
 * Copyright (c) 2016-2021.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.client.event;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.client.gui.screens.Screen;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import org.lwjgl.glfw.GLFW;

/**
 * Event classes for GuiScreen events.
 *
 * @author bspkrs
 */
@OnlyIn(Dist.CLIENT)
public class GuiScreenEvent extends Event
{
    private final Screen gui;

    public GuiScreenEvent(Screen gui)
    {
        this.gui = gui;
    }

    /**
     * The GuiScreen object generating this event.
     */
    public Screen getGui()
    {
        return gui;
    }

    public static class InitGuiEvent extends GuiScreenEvent
    {
        private Consumer<GuiEventListener> add;
        private Consumer<GuiEventListener> remove;

        private List<GuiEventListener> list;

        public InitGuiEvent(Screen gui, List<GuiEventListener> list, Consumer<GuiEventListener> add, Consumer<GuiEventListener> remove)
        {
            super(gui);
            this.list = Collections.unmodifiableList(list);
            this.add = add;
            this.remove = remove;
        }

        /**
         * Unmodifiable reference to the list of buttons on the {@link #gui}.
         */
        public List<GuiEventListener> getWidgetList()
        {
            return list;
        }

        public void addWidget(GuiEventListener button)
        {
            add.accept(button);
        }

        public void removeWidget(GuiEventListener button)
        {
            remove.accept(button);
        }

        /**
         * This event fires just after initializing the {@link Minecraft}, font renderer, width,
         * and height fields.<br/><br/>
         *
         * If canceled the following lines are skipped in {@link Screen#init(Minecraft, int, int)}:<br/>
         * {@code this.buttonList.clear();}<br/>
         * {@code this.children.clear();}<br/>
         * {@code this.initGui();}<br/>
         */
        @Cancelable
        public static class Pre extends InitGuiEvent
        {
            public Pre(Screen gui, List<GuiEventListener> list, Consumer<GuiEventListener> add, Consumer<GuiEventListener> remove)
            {
                super(gui, list, add, remove);
            }
        }

        /**
         * This event fires right after the {@code init()} method in {@link Screen}.
         * This is a good place to alter a GuiScreen's component layout if desired.
         */
        public static class Post extends InitGuiEvent
        {
            public Post(Screen gui, List<GuiEventListener> list, Consumer<GuiEventListener> add, Consumer<GuiEventListener> remove)
            {
                super(gui, list, add, remove);
            }
        }
    }

    public static class DrawScreenEvent extends GuiScreenEvent
    {
        private final PoseStack mStack;
        private final int mouseX;
        private final int mouseY;
        private final float renderPartialTicks;

        public DrawScreenEvent(Screen gui, PoseStack mStack, int mouseX, int mouseY, float renderPartialTicks)
        {
            super(gui);
            this.mStack = mStack;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.renderPartialTicks = renderPartialTicks;
        }

        /**
         * The MatrixStack to render with.
         */
        public PoseStack getMatrixStack()
        {
            return mStack;
        }

        /**
         * The x coordinate of the mouse pointer on the screen.
         */
        public int getMouseX()
        {
            return mouseX;
        }

        /**
         * The y coordinate of the mouse pointer on the screen.
         */
        public int getMouseY()
        {
            return mouseY;
        }

        /**
         * Partial render ticks elapsed.
         */
        public float getRenderPartialTicks()
        {
            return renderPartialTicks;
        }

        /**
         * This event fires just before {@link Screen#render(PoseStack, int, int, float)} is called.
         * Cancel this event to skip the render method.
         */
        @Cancelable
        public static class Pre extends DrawScreenEvent
        {
            public Pre(Screen gui, PoseStack mStack, int mouseX, int mouseY, float renderPartialTicks)
            {
                super(gui, mStack, mouseX, mouseY, renderPartialTicks);
            }
        }

        /**
         * This event fires just after {@link Screen#render(PoseStack, int, int, float)} is called.
         */
        public static class Post extends DrawScreenEvent
        {
            public Post(Screen gui, PoseStack mStack, int mouseX, int mouseY, float renderPartialTicks)
            {
                super(gui, mStack, mouseX, mouseY, renderPartialTicks);
            }
        }
    }

    /**
     * This event fires at the end of {@link Screen#renderBackground(PoseStack, int)} and before the rest of the Gui draws.
     * This allows drawing next to Guis, above the background but below any tooltips.
     */
    public static class BackgroundDrawnEvent extends GuiScreenEvent
    {
        private final PoseStack mStack;

        public BackgroundDrawnEvent(Screen gui, PoseStack mStack)
        {
            super(gui);
            this.mStack = mStack;
        }

        /**
         * The MatrixStack to render with.
         */
        public PoseStack getMatrixStack()
        {
            return mStack;
        }
    }

    /**
     * This event fires in {@link net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen} in the
     * {@code checkEffectRendering} method when potion effects are active and the gui wants to move over.
     * Cancel this event to prevent the Gui from being moved.
     */
    @Cancelable
    public static class PotionShiftEvent extends GuiScreenEvent
    {
        public PotionShiftEvent(Screen gui)
        {
            super(gui);
        }
    }

    public static abstract class MouseInputEvent extends GuiScreenEvent
    {
        private final double mouseX;
        private final double mouseY;

        public MouseInputEvent(Screen gui, double mouseX, double mouseY)
        {
            super(gui);
            this.mouseX = mouseX;
            this.mouseY = mouseY;
        }

        public double getMouseX()
        {
            return mouseX;
        }

        public double getMouseY()
        {
            return mouseY;
        }
    }

    public static abstract class MouseClickedEvent extends MouseInputEvent
    {
        private final int button;

        public MouseClickedEvent(Screen gui, double mouseX, double mouseY, int button)
        {
            super(gui, mouseX, mouseY);
            this.button = button;
        }

        public int getButton()
        {
            return button;
        }

        /**
         * This event fires when a mouse click is detected for a GuiScreen, before it is handled.
         * Cancel this event to bypass {@link GuiEventListener#mouseClicked(double, double, int)}.
         */
        @Cancelable
        public static class Pre extends MouseClickedEvent
        {
            public Pre(Screen gui, double mouseX, double mouseY, int button)
            {
                super(gui, mouseX, mouseY, button);
            }
        }

        /**
         * This event fires after {@link GuiEventListener#mouseClicked(double, double, int)} if the click was not already handled.
         * Cancel this event when you successfully use the mouse click, to prevent other handlers from using the same input.
         */
        @Cancelable
        public static class Post extends MouseClickedEvent
        {
            public Post(Screen gui, double mouseX, double mouseY, int button)
            {
                super(gui, mouseX, mouseY, button);
            }
        }
    }

    public static abstract class MouseReleasedEvent extends MouseInputEvent
    {
        private final int button;

        public MouseReleasedEvent(Screen gui, double mouseX, double mouseY, int button)
        {
            super(gui, mouseX, mouseY);
            this.button = button;
        }

        public int getButton()
        {
            return button;
        }

        /**
         * This event fires when a mouse release is detected for a GuiScreen, before it is handled.
         * Cancel this event to bypass {@link GuiEventListener#mouseReleased(double, double, int)}.
         */
        @Cancelable
        public static class Pre extends MouseReleasedEvent
        {
            public Pre(Screen gui, double mouseX, double mouseY, int button)
            {
                super(gui, mouseX, mouseY, button);
            }
        }

        /**
         * This event fires after {@link GuiEventListener#mouseReleased(double, double, int)} if the release was not already handled.
         * Cancel this event when you successfully use the mouse release, to prevent other handlers from using the same input.
         */
        @Cancelable
        public static class Post extends MouseReleasedEvent
        {
            public Post(Screen gui, double mouseX, double mouseY, int button)
            {
                super(gui, mouseX, mouseY, button);
            }
        }
    }

    public static abstract class MouseDragEvent extends MouseInputEvent
    {
        private final int mouseButton;
        private final double dragX;
        private final double dragY;

        public MouseDragEvent(Screen gui, double mouseX, double mouseY, int mouseButton, double dragX, double dragY)
        {
            super(gui, mouseX, mouseY);
            this.mouseButton = mouseButton;
            this.dragX = dragX;
            this.dragY = dragY;
        }

        public int getMouseButton()
        {
            return mouseButton;
        }

        public double getDragX()
        {
            return dragX;
        }

        public double getDragY()
        {
            return dragY;
        }

        /**
         * This event fires when a mouse drag is detected for a GuiScreen, before it is handled.
         * Cancel this event to bypass {@link GuiEventListener#mouseDragged(double, double, int, double, double)}.
         */
        @Cancelable
        public static class Pre extends MouseDragEvent
        {
            public Pre(Screen gui, double mouseX, double mouseY, int mouseButton, double dragX, double dragY)
            {
                super(gui, mouseX, mouseY, mouseButton, dragX, dragY);
            }
        }

        /**
         * This event fires after {@link GuiEventListener#mouseDragged(double, double, int, double, double)} if the drag was not already handled.
         * Cancel this event when you successfully use the mouse drag, to prevent other handlers from using the same input.
         */
        @Cancelable
        public static class Post extends MouseDragEvent
        {
            public Post(Screen gui, double mouseX, double mouseY, int mouseButton, double dragX, double dragY)
            {
                super(gui, mouseX, mouseY, mouseButton, dragX, dragY);
            }
        }
    }

    public static abstract class MouseScrollEvent extends MouseInputEvent
    {
        private final double scrollDelta;

        public MouseScrollEvent(Screen gui, double mouseX, double mouseY, double scrollDelta)
        {
            super(gui, mouseX, mouseY);
            this.scrollDelta = scrollDelta;
        }

        public double getScrollDelta()
        {
            return scrollDelta;
        }

        /**
         * This event fires when a mouse scroll is detected for a GuiScreen, before it is handled.
         * Cancel this event to bypass {@link GuiEventListener#mouseScrolled(double, double, double)}.
         */
        @Cancelable
        public static class Pre extends MouseScrollEvent
        {
            public Pre(Screen gui, double mouseX, double mouseY, double scrollDelta)
            {
                super(gui, mouseX, mouseY, scrollDelta);
            }
        }

        /**
         * This event fires after {@link GuiEventListener#mouseScrolled(double, double, double)} if the scroll was not already handled.
         * Cancel this event when you successfully use the mouse scroll, to prevent other handlers from using the same input.
         */
        @Cancelable
        public static class Post extends MouseScrollEvent
        {
            public Post(Screen gui, double mouseX, double mouseY, double scrollDelta)
            {
                super(gui, mouseX, mouseY, scrollDelta);
            }
        }
    }

    public static abstract class KeyboardKeyEvent extends GuiScreenEvent
    {
        private final int keyCode;
        private final int scanCode;
        private final int modifiers;

        public KeyboardKeyEvent(Screen gui, int keyCode, int scanCode, int modifiers)
        {
            super(gui);
            this.keyCode = keyCode;
            this.scanCode = scanCode;
            this.modifiers = modifiers;
        }

        /**
         * The keyboard key that was pressed or released
         * https://www.glfw.org/docs/latest/group__keys.html
         *
         * @see GLFW key constants starting with "GLFW_KEY_"
         */
        public int getKeyCode()
        {
            return keyCode;
        }

        /**
         * Platform-specific scan code.
         * Used for {@link com.mojang.blaze3d.platform.InputConstants#getKey(int, int)}
         *
         * The scan code is unique for every key, regardless of whether it has a key code.
         * Scan codes are platform-specific but consistent over time, so keys will have different scan codes depending
         * on the platform but they are safe to save to disk as custom key bindings.
         */
        public int getScanCode()
        {
            return scanCode;
        }

        /**
         * Bit field representing the modifier keys pressed.
         * https://www.glfw.org/docs/latest/group__mods.html
         *
         * @see GLFW#GLFW_MOD_SHIFT
         * @see GLFW#GLFW_MOD_CONTROL
         * @see GLFW#GLFW_MOD_ALT
         * @see GLFW#GLFW_MOD_SUPER
         */
        public int getModifiers()
        {
            return modifiers;
        }
    }

    public static abstract class KeyboardKeyPressedEvent extends KeyboardKeyEvent
    {
        public KeyboardKeyPressedEvent(Screen gui, int keyCode, int scanCode, int modifiers)
        {
            super(gui,  keyCode, scanCode, modifiers);
        }

        /**
         * This event fires when keyboard input is detected for a GuiScreen, before it is handled.
         * Cancel this event to bypass {@link GuiEventListener#keyPressed(int, int, int)}.
         */
        @Cancelable
        public static class Pre extends KeyboardKeyPressedEvent
        {
            public Pre(Screen gui, int keyCode, int scanCode, int modifiers)
            {
                super(gui, keyCode, scanCode, modifiers);
            }
        }

        /**
         * This event fires after {@link GuiEventListener#keyPressed(int, int, int)} if the key was not already handled.
         * Cancel this event when you successfully use the keyboard input to prevent other handlers from using the same input.
         */
        @Cancelable
        public static class Post extends KeyboardKeyPressedEvent
        {
            public Post(Screen gui, int keyCode, int scanCode, int modifiers)
            {
                super(gui, keyCode, scanCode, modifiers);
            }
        }
    }

    public static abstract class KeyboardKeyReleasedEvent extends KeyboardKeyEvent
    {
        public KeyboardKeyReleasedEvent(Screen gui, int keyCode, int scanCode, int modifiers)
        {
            super(gui, keyCode, scanCode, modifiers);
        }

        /**
         * This event fires when keyboard input is detected for a GuiScreen, before it is handled.
         * Cancel this event to bypass {@link GuiEventListener#keyReleased(int, int, int)}.
         */
        @Cancelable
        public static class Pre extends KeyboardKeyReleasedEvent
        {
            public Pre(Screen gui, int keyCode, int scanCode, int modifiers)
            {
                super(gui, keyCode, scanCode, modifiers);
            }
        }

        /**
         * This event fires after {@link GuiEventListener#keyReleased(int, int, int)} if the key was not already handled.
         * Cancel this event when you successfully use the keyboard input to prevent other handlers from using the same input.
         */
        @Cancelable
        public static class Post extends KeyboardKeyReleasedEvent
        {
            public Post(Screen gui, int keyCode, int scanCode, int modifiers)
            {
                super(gui, keyCode, scanCode, modifiers);
            }
        }
    }

    public static class KeyboardCharTypedEvent extends GuiScreenEvent
    {
        private final char codePoint;
        private final int modifiers;

        public KeyboardCharTypedEvent(Screen gui, char codePoint, int modifiers)
        {
            super(gui);
            this.codePoint = codePoint;
            this.modifiers = modifiers;
        }

        /**
         * The code point typed, used for text entry.
         */
        public char getCodePoint()
        {
            return codePoint;
        }

        /**
         * Bit field representing the modifier keys pressed.
         *
         * @see GLFW#GLFW_MOD_SHIFT
         * @see GLFW#GLFW_MOD_CONTROL
         * @see GLFW#GLFW_MOD_ALT
         * @see GLFW#GLFW_MOD_SUPER
         */
        public int getModifiers()
        {
            return modifiers;
        }

        /**
         * This event fires when keyboard character input is detected for a GuiScreen, before it is handled.
         * Cancel this event to bypass {@link GuiEventListener#charTyped(char, int)}.
         */
        @Cancelable
        public static class Pre extends KeyboardCharTypedEvent
        {
            public Pre(Screen gui, char codePoint, int modifiers)
            {
                super(gui, codePoint, modifiers);
            }
        }

        /**
         * This event fires after {@link GuiEventListener#charTyped(char, int)} if the character was not already handled.
         * Cancel this event when you successfully use the keyboard input to prevent other handlers from using the same input.
         */
        @Cancelable
        public static class Post extends KeyboardCharTypedEvent
        {
            public Post(Screen gui, char codePoint, int modifiers)
            {
                super(gui, codePoint, modifiers);
            }
        }
    }
}
