package com.lifelately.service;

import com.lifelately.dao.MoodDAO;
import com.lifelately.model.Mood;

import java.util.List;

public final class MoodService {
    private final MoodDAO moodDAO;

    public MoodService(MoodDAO moodDAO) {
        this.moodDAO = moodDAO;
    }

    public List<Mood> getMoods() {
        return moodDAO.findAll();
    }
}
