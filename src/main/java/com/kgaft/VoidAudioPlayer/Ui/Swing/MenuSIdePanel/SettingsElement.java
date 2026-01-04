package com.kgaft.VoidAudioPlayer.Ui.Swing.MenuSIdePanel;

import com.kgaft.VoidAudioPlayer.Ui.StringUtils.StringManager;
import com.kgaft.VoidAudioPlayer.Ui.SwingUtil.IconInflater;

import javax.swing.*;
import java.awt.*;

public class SettingsElement implements SidePanelElement{
    @Override
    public JPanel getEntryButton() {
        return makeEntryButton();
    }

    @Override
    public JPanel contentPanel() {
        return new JPanel();
    }

    private JPanel makeEntryButton(){
        Icon icon = IconInflater.loadIcon("icons/settings.svg", ICON_SIZE_PREFERRED.width, ICON_SIZE_PREFERRED.height);
        JPanel res = new JPanel();
        res.setPreferredSize(WHOLE_SIZE_PREFERRED);
        res.setLayout(new BoxLayout(res, BoxLayout.Y_AXIS));
        res.add(new JLabel(icon));
        res.add(new JLabel(StringManager.getString("settings")));
        return res;
    }
}
