# Employee Directory

A small REST API to manage employees. It is made with Java 21, Spring Boot 4.1.1, Spring Data JPA and MySQL.

## Database setup

1. Install MySQL and start it (default port 3306).
2. Open `src/main/resources/application.properties` and write your own MySQL username and password:

       spring.datasource.username=root
       spring.datasource.password=your_mysql_password

3. You do not need to create the database or the table by yourself. When the app starts for the first time, it creates the database `employee_crud` and the `employees` table.

## How to run

You need JDK 21 or higher. Maven is not needed because the project has the Maven wrapper.

    ./mvnw spring-boot:run

On Windows Command Prompt use `mvnw.cmd spring-boot:run`. The app runs on http://localhost:8080.

To run the tests use `./mvnw test`. The tests use an H2 in-memory database, so MySQL is not needed for them.

## API endpoints

- POST /api/employees : create an employee (201)
- GET /api/employees : get all employees (200)
- GET /api/employees/{id} : get one employee (200)
- PUT /api/employees/{id} : update an employee (200)
- DELETE /api/employees/{id} : delete an employee (204)

## Sample requests

I tested the API with Postman. For POST and PUT, choose Body, then raw, then JSON.

### Create an employee

- Method: POST
- URL: `http://localhost:8080/api/employees`
- Body:

```json
{
  "fullName": "Karim Hasan",
  "email": "karim@example.com",
  "phone": "01712345678",
  "department": "Engineering",
  "designation": "Intern",
  "joiningDate": "2026-10-01"
}
```

- Result: 201 Created, with the saved employee (id, createdAt and the other fields).

### Get all employees

- Method: GET
- URL: `http://localhost:8080/api/employees`
- Result: 200 OK, a list of employees.

### Get one employee

- Method: GET
- URL: `http://localhost:8080/api/employees/1`
- Result: 200 OK, one employee. If the id does not exist, 404.

### Update an employee

- Method: PUT
- URL: `http://localhost:8080/api/employees/1`
- Body (same fields as create, here the designation is changed):

```json
{
  "fullName": "Karim Hasan",
  "email": "karim@example.com",
  "phone": "01712345678",
  "department": "Engineering",
  "designation": "Junior Developer",
  "joiningDate": "2026-10-01"
}
```

- Result: 200 OK, with the updated employee (updatedAt is set now).

### Delete an employee

- Method: DELETE
- URL: `http://localhost:8080/api/employees/1`
- Result: 204 No Content (empty body).

## Validation and errors

- fullName is required, 3 to 100 characters.
- email is required, must be a valid email and must be unique.
- phone is required, 11 digits, starts with 01.
- department is required. designation is optional.
- joiningDate is required, format yyyy-MM-dd, and it cannot be a future date.

If the input is wrong, the API returns 400 with a message for each wrong field. If the id does not exist, it returns 404. If the email is already used (create or update), it returns 409. All errors are returned as JSON.


## File Upload

An employee can have one file, either a PDF or an image. The file is saved in the database, in the same employee row (columns `file_name`, `file_type` and `file_data`). These columns are created automatically when the app starts, so nothing extra is needed.

Allowed files are PDF, JPEG, PNG, GIF and WebP, up to 5MB. If you upload a new file for the same employee, the old one is replaced. The file itself is never sent inside the JSON. The employee response only shows `fileName`, `fileType` and `hasFile`.

| Method | URL | What it does | Success |
|---|---|---|---|
| POST | /api/employees/{id}/file | Upload or replace the file | 200 |
| GET | /api/employees/{id}/file | Open or download the file | 200 |
| DELETE | /api/employees/{id}/file | Remove the file, employee stays | 204 |

### Sample requests (Postman)

Upload a file:

- Method: POST
- URL: `http://localhost:8080/api/employees/1/file`
- Body: choose `form-data`, add a key named `file`, change its type from Text to File, then select a PDF or image.

Response (200):

```json
{
  "id": 1,
  "fullName": "Karim Hasan",
  "email": "karim@example.com",
  "phone": "01712345678",
  "department": "Engineering",
  "designation": "Intern",
  "joiningDate": "2026-10-01",
  "createdAt": "2026-10-18T14:30:12.123456",
  "updatedAt": "2026-10-18T14:35:40.654321",
  "fileName": "certificate.pdf",
  "fileType": "application/pdf",
  "hasFile": true
}
```

Get the file:

- Method: GET
- URL: `http://localhost:8080/api/employees/1/file`
- No body. Postman shows the image, or the PDF, in the response.

Remove the file:

- Method: DELETE
- URL: `http://localhost:8080/api/employees/1/file`
- No body. Response is 204 with nothing in it.

### Errors

| When | Status | Message |
|---|---|---|
| No file sent, or the file is empty | 400 | Please select a file to upload |
| File is not a PDF or an image | 400 | Only PDF and image files are allowed |
| File is bigger than 5MB | 400 | File size must not exceed 5MB |
| File name is longer than 255 characters | 400 | File name must not exceed 255 characters |
| Employee id does not exist | 404 | Employee not found with id: {id} |
| Employee has no file (GET or DELETE) | 404 | No file found for employee id: {id} |

## Frontend

I made a simple frontend using plain HTML, CSS and JavaScript (no Angular or React). It shows the employee list and has add, edit and delete.

The files are in `src/main/resources/static` (`index.html`, `style.css`, `app.js`). Spring Boot serves them by itself, so there is nothing extra to install.

How to run it:

1. Start the project with `./mvnw spring-boot:run` (see "How to run" above).
2. Open http://localhost:8080 in your browser.
3. The Add and Edit form has a file field for a PDF or image up to 5MB. The browser checks the type and size first and the server checks again.
4. The employee list has View and Download links for the file. The Edit form shows the current file with the same links.

If a field is wrong, the error message from the API is shown under that field. If the email already exists, it is shown under the email field.