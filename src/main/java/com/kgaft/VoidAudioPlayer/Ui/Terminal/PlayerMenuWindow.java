package com.kgaft.VoidAudioPlayer.Ui.Terminal;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Panel;
import com.kgaft.VoidAudioPlayer.Ui.StringUtils.StringManager;
import com.kgaft.VoidAudioPlayer.Ui.Terminal.CollectionPanel.CollectionPanel;

public class PlayerMenuWindow {
    public PlayerMenuWindow(PlayerTerminal terminal) {
        BasicWindow window = new BasicWindow(StringManager.getString("app_name"));

        Panel panel = new Panel();

        ActionListBox list = new ActionListBox(new TerminalSize(20, 10));
        list.addItem(StringManager.getString("settings"), () -> window.setComponent(new SettingsPanel(window, panel)));
        list.addItem(StringManager.getString("collection"), () -> window.setComponent(new CollectionPanel(window, panel)));
        list.addItem(StringManager.getString("player_controls"), () -> new PlayerWindow(terminal));

        panel.addComponent(list);
        panel.addComponent(new Button(StringManager.getString("exit"), window::close));

        window.setComponent(panel);
        terminal.getGui().addWindow(window);
        terminal.registerWindow(window);
    }
}
