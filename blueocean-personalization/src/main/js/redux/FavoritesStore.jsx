/**
 * Created by cmeyers on 7/6/16.
 */

import keymirror from 'keymirror';
import Immutable from 'immutable';
import { createSelector } from 'reselect';

import { AnonUser, User } from '../model/User';
import { checkMatchingFavoriteUrls } from '../util/FavoriteUtils';

/* eslint new-cap: [0] */
const { Record, List } = Immutable;

export const FavoritesState = Record({
    user: null,
    favorites: null,
});

export const ACTION_TYPES = keymirror({
    SET_USER: null,
    SET_FAVORITES: null,
    TOGGLE_FAVORITE: null,
    UPDATE_RUN: null,
});

function clone(json) {
    return JSON.parse(JSON.stringify(json));
}

const actionHandlers = {
    [ACTION_TYPES.SET_USER](state, { payload }) {
        const user = payload instanceof Error ?
            new AnonUser() :
            new User(payload);
        return state.set('user', user);
    },
    [ACTION_TYPES.SET_FAVORITES](state, { payload }) {
        const favoriteList = new List(payload);
        return state.set('favorites', favoriteList);
    },
    [ACTION_TYPES.TOGGLE_FAVORITE](state, { addFavorite, branch, payload }) {
        const favoritesList = state.get('favorites');

        if (addFavorite) {
            const appendedList = favoritesList.push(payload);
            return state.set('favorites', appendedList);
        }

        const toggledBranchHref = branch._links.self.href;
        // filter the list so that only favorites which didn't match the branch's href are returned
        const prunedList = favoritesList.filter(fav => {
            const favoritedBranch = fav.item;
            return !checkMatchingFavoriteUrls(
                favoritedBranch._links.self.href,
                toggledBranchHref,
            );
        });

        return state.set('favorites', prunedList);
    },
    [ACTION_TYPES.UPDATE_RUN](state, { jobRun }) {
        const favorites = state.get('favorites');

        for (const fav of favorites) {
            const runsBaseUrl = `${fav.item._links.self.href}runs`;
            const runUrl = jobRun._links.self.href;

            // if the job's run URL starts with the favorited item's '/runs' URL,
            // then the run applies to that item, so update the 'latestRun' property
            if (runUrl.indexOf(runsBaseUrl) === 0) {
                const index = favorites.indexOf(fav);
                const updatedFavorite = clone(fav);
                updatedFavorite.item.latestRun = jobRun;
                const updatedFavorites = favorites.set(index, updatedFavorite);
                return state.set('favorites', updatedFavorites);
            }
        }

        return state;
    },
};

const favoritesStore = state => state.favoritesStore;
export const userSelector = createSelector([favoritesStore], store => store.user);
export const favoritesSelector = createSelector([favoritesStore], store => store.favorites);

// reducer
function reducer(state = new FavoritesState(), action):FavoritesState {
    const { type } = action;
    if (type in actionHandlers) {
        return actionHandlers[type](state, action);
    }
    return state;
}

export default {
    favoritesStore: reducer,
};
