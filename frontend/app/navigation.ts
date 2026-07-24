export type Route =
  | { screen: "hub" }
  | { screen: "parts" }
  | { screen: "part"; partId: number }
  | { screen: "part-edit"; partId: number }
  | { screen: "part-gallery"; partId: number }
  | { screen: "tech-process"; partId: number }
  | { screen: "setup"; partId: number; setupId: number }
  | { screen: "assembly"; partId: number }
  | { screen: "catalog" };

export function routeTitle(route: Route): string {
  switch (route.screen) {
    case "hub":
      return "Техпроцессы ЧПУ";
    case "parts":
      return "Детали";
    case "part":
      return "Карточка детали";
    case "part-edit":
      return "Изменить деталь";
    case "part-gallery":
      return "Фото";
    case "tech-process":
      return "Техпроцесс";
    case "setup":
      return "Установ";
    case "assembly":
      return "Сборка";
    case "catalog":
      return "Инструмент";
  }
}
