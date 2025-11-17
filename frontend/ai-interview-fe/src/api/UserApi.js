import Https from "../access/Https";

export const UserApi = {
  Get_Profile: () => Https.get("/api/users"),
  Update_Picture: (body) => Https.put("/api/users/update-picture", body),
};