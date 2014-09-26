/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.inputmethod.latin.personalization;

import android.content.Context;
import android.text.TextUtils;

import com.android.inputmethod.annotations.UsedForTesting;
import com.android.inputmethod.latin.Constants;
import com.android.inputmethod.latin.Dictionary;
import com.android.inputmethod.latin.ExpandableBinaryDictionary;
import com.android.inputmethod.latin.PrevWordsInfo;
import com.android.inputmethod.latin.utils.DistracterFilter;

import java.io.File;
import java.util.Locale;

/**
 * Locally gathers stats about the words user types and various other signals like auto-correction
 * cancellation or manual picks. This allows the keyboard to adapt to the typist over time.
 */
public class UserHistoryDictionary extends DecayingExpandableBinaryDictionaryBase {
    /* package */ static final String NAME = UserHistoryDictionary.class.getSimpleName();
    private final static int SUPPORTED_NGRAM = 2; // TODO: 3

    // TODO: Make this constructor private
    /* package */ UserHistoryDictionary(final Context context, final Locale locale) {
        super(context, getDictName(NAME, locale, null /* dictFile */), locale,
                Dictionary.TYPE_USER_HISTORY, null /* dictFile */);
    }

    @UsedForTesting
    public static UserHistoryDictionary getDictionary(final Context context, final Locale locale,
            final File dictFile, final String dictNamePrefix) {
        return PersonalizationHelper.getUserHistoryDictionary(context, locale);
    }

    /**
     * Add a word to the user history dictionary.
     *
     * @param userHistoryDictionary the user history dictionary
     * @param prevWordsInfo the information of previous words
     * @param word the word the user inputted
     * @param isValid whether the word is valid or not
     * @param timestamp the timestamp when the word has been inputted
     * @param distracterFilter the filter to check whether the word is a distracter
     */
    public static void addToDictionary(final ExpandableBinaryDictionary userHistoryDictionary,
            final PrevWordsInfo prevWordsInfo, final String word, final boolean isValid,
            final int timestamp, final DistracterFilter distracterFilter) {
        if (word.length() > Constants.DICTIONARY_MAX_WORD_LENGTH) {
            return;
        }
        final int frequency = isValid ?
                FREQUENCY_FOR_WORDS_IN_DICTS : FREQUENCY_FOR_WORDS_NOT_IN_DICTS;
        userHistoryDictionary.addUnigramEntryWithCheckingDistracter(word, frequency,
                null /* shortcutTarget */, 0 /* shortcutFreq */, false /* isNotAWord */,
                false /* isBlacklisted */, timestamp, distracterFilter);

        final boolean isBeginningOfSentenceContext = prevWordsInfo.isBeginningOfSentenceContext();
        final PrevWordsInfo prevWordsInfoToBeSaved =
                prevWordsInfo.getTrimmedPrevWordsInfo(SUPPORTED_NGRAM - 1);
        for (int i = 0; i < prevWordsInfoToBeSaved.getPrevWordCount(); i++) {
            final CharSequence prevWord = prevWordsInfoToBeSaved.getNthPrevWord(1 /* n */);
            if (prevWord == null || (prevWord.length() > Constants.DICTIONARY_MAX_WORD_LENGTH)) {
                return;
            }
            // Do not insert a word as a bigram of itself
            if (i == 0 && TextUtils.equals(word, prevWord)) {
                return;
            }
            if (isBeginningOfSentenceContext) {
                // Beginning-of-Sentence n-gram entry is added as an n-gram entry of an OOV word.
                userHistoryDictionary.addNgramEntry(
                        prevWordsInfoToBeSaved.getTrimmedPrevWordsInfo(i + 1), word,
                        FREQUENCY_FOR_WORDS_NOT_IN_DICTS, timestamp);
            } else {
                userHistoryDictionary.addNgramEntry(
                        prevWordsInfoToBeSaved.getTrimmedPrevWordsInfo(i + 1), word, frequency,
                        timestamp);
            }
        }
    }
}
