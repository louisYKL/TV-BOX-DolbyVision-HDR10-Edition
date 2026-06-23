package com.github.tvbox.osc.player;

import android.content.Context;

import xyz.doikki.videoplayer.player.PlayerFactory;

public class MPVCompatPlayerFactory extends PlayerFactory<MPVCompatPlayer> {
    public static MPVCompatPlayerFactory create() {
        return new MPVCompatPlayerFactory();
    }

    @Override
    public MPVCompatPlayer createPlayer(Context context) {
        return new MPVCompatPlayer(context);
    }
}
