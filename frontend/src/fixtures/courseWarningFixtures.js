const showOrganizationAgeWarning = {
  showOrganizationAgeWarning: true,
  defaultBasePermission: "none",
};

const hideOrganizationAgeWarning = {
  showOrganizationAgeWarning: false,
  defaultBasePermission: "none",
};

const readBasePermission = {
  showOrganizationAgeWarning: false,
  defaultBasePermission: "read",
};

const writeBasePermission = {
  showOrganizationAgeWarning: false,
  defaultBasePermission: "write",
};

const adminBasePermission = {
  showOrganizationAgeWarning: false,
  defaultBasePermission: "admin",
};

const bothWarnings = {
  showOrganizationAgeWarning: true,
  defaultBasePermission: "read",
};

const noOrgLinked = {
  showOrganizationAgeWarning: false,
  defaultBasePermission: "null",
};

const warningHidden = {
  showOrganizationAgeWarning: false,
  defaultBasePermission: "read",
  hideBasePermissionWarning: true,
};

export {
  showOrganizationAgeWarning,
  hideOrganizationAgeWarning,
  readBasePermission,
  writeBasePermission,
  adminBasePermission,
  bothWarnings,
  noOrgLinked,
  warningHidden,
};
