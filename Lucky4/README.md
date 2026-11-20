# Lucky 4 — Spring Boot Group Profile

A casino/slot-machine inspired group profile system for a school project. It uses Java 17 + Spring Boot + HTML/CSS/JavaScript.

## Requirements
- Java 17+
- Maven 3.6.3+
- VS Code (or IntelliJ IDEA)

## Run in VS Code
Open the **Lucky4** folder (the one containing `pom.xml`) in VS Code, then in the terminal:

```powershell
mvn spring-boot:run
```

Then open http://localhost:8080

(This project does not include the Maven Wrapper. To add it, run `mvn wrapper:wrapper` once, then you can use `mvnw.cmd spring-boot:run`.)

## Run the tests
```powershell
mvn test
```

## Settings
`src/main/resources/application.properties`
- `server.port` - change the port if 8080 is busy
- `lucky4.jackpot-chance` - chance (0.0 to 1.0) that a spin is a jackpot. Default `0.25`.

## Build a JAR
```powershell
mvn clean package
```

Then:
```powershell
java -jar target/lucky4-1.0.0.jar
```

## Edit the team (names, details, photos)
Open http://localhost:8080/edit.html (it is not linked from the game page).

- Enter the edit code. It is `lucky4` by default. **Change it** in `application.properties` (`lucky4.edit-code`). Leave it empty to turn the code off.
- Change any detail, choose a new photo, and press **Save**.
- Edits are saved in the `data/` folder next to `pom.xml` (`data/members.properties` and `data/uploads/`), so they survive restarts. Delete the `data/` folder to go back to the defaults.
- The starting values live in `src/main/java/com/example/lucky4/service/MemberStore.java`.

Anyone who can reach your server and knows the code can edit the team, so keep the code private.

## Customize the memes
Replace the meme SVGs in `src/main/resources/static/media/` with your own funny images.

## Spring components used
- `@SpringBootApplication` — starts the application
- `@RestController` — exposes the API endpoints
- `@RequestMapping` — groups API routes under `/api`
- `@GetMapping` — handles GET requests
- `@Service` — holds game logic
- Java records — model API data

## API routes
- GET `/api/spin`
- GET `/api/members`
- GET `/api/question`
- GET `/api/edit/check`, PUT `/api/edit/members/{id}`, POST `/api/edit/members/{id}/photo` (used by the edit page; need the `X-Edit-Code` header)

## Project notes
- Losing spins never show four identical faces, and a jackpot always shows four of the same member.
- The "Meet the team" section is built from `/api/members`.