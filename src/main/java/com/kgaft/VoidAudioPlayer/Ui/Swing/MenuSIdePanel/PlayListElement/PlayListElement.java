package com.kgaft.VoidAudioPlayer.Ui.Swing.MenuSIdePanel.PlayListElement;

import com.kgaft.VoidAudioPlayer.Ui.StringUtils.StringManager;
import com.kgaft.VoidAudioPlayer.Ui.Swing.MenuSIdePanel.SidePanelElement;
import com.kgaft.VoidAudioPlayer.Ui.SwingUtil.IconInflater;

import javax.swing.*;

public class PlayListElement implements SidePanelElement {

    @Override
    public JPanel getEntryButton() {
        return makeEntryButton();
    }

    @Override
    public JPanel contentPanel() {
        return new JPanel();
    }

    private JPanel makeEntryButton(){
        Icon icon = IconInflater.loadIcon("icons/playlist.svg", ICON_SIZE_PREFERRED.width, ICON_SIZE_PREFERRED.height);
        JPanel res = new JPanel();
        res.setLayout(new BoxLayout(res, BoxLayout.Y_AXIS));
        res.setPreferredSize(WHOLE_SIZE_PREFERRED);
        res.add(new JLabel(icon));
        res.add(new JLabel(StringManager.getString("playlist")));
        return res;
    }
}
