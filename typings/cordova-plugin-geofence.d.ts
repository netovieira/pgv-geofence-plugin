interface Window {
  geofence: GeofencePlugin;
}

interface GeofencePlugin {
  initialize(
    successCallback?: (result: any) => void,
    errorCallback?: (error: string) => void
  ): Promise<any>;

  setUserInfo(
    userInfo,
    successCallback?: (result: any) => void,
    errorCallback?: (error: string) => void
  ): Promise<any>;
}
