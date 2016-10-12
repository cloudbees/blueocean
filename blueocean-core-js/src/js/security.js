/**
 * Created by cmeyers on 9/16/16.
 */

/**
 * Returns a key of permissions functions that each return boolean to indicate authorization.
 * Usage:
 *      permit(pipeline).create();
 *
 * @param subject
 * @returns {{read: (function()), create: (function()), start: (function()), stop: (function())}}
 */
const permit = (subject) => {
    const checkPermissions = (permissionName) => (
        subject && subject.permissions && !!subject.permissions[permissionName]
    );

    return {
        read: () => checkPermissions('read'),
        create: () => checkPermissions('create'),
        start: () => checkPermissions('start'),
        stop: () => checkPermissions('stop'),
    };
};

export default {
    permit,
};
