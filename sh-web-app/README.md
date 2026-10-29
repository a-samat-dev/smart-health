# sh-web-app

React app for patients and doctors. It talks only to sh-api-gateway, which reaches each service under its
name, such as `/doctor-service/api/v1/doctors`.

## Pages

| Path                         | What it shows                                                              |
|------------------------------|----------------------------------------------------------------------------|
| `/sign-up`, `/sign-up/doctor`| Creating an account as a patient or as a doctor; doctors go on to register |
| `/sign-in`                   | Signing in                                                                 |
| `/profile`                   | Your account, and your doctor profile or a link to register as a doctor    |
| `/doctor-registration`       | License number and PDF, specializations, practice address and contacts, working hours, days off |
| `/doctor-profile/edit`       | Changing the doctor profile and replacing the license PDF                 |
| `/doctors`                   | Doctors, by specialization                                                 |
| `/doctors/{id}/book`         | A week of the doctor's slots to book one; with `?appointment={id}`, to move that appointment |
| `/appointments`              | Your appointments by day, week or month, like a calendar, or as a list with search by specialization, sorting, a date range and "Load more" |

Times are shown in the browser's time zone. The calendar views load every appointment in the period they
show; the list loads ten at a time with the API's cursor.

## Running

You need Node 22.22 or later.

```bash
npm install
```

```bash
npm run dev
```

The app runs on http://localhost:5173, the origin the gateway allows by default. To send requests somewhere
other than `http://localhost:8080`, copy `.env.example` to `.env.local` and set `VITE_API_URL`.

## Tokens

Signing up creates the account (`POST /user-service/api/v1/users`) and then signs in (`POST /user-service/api/v1/sessions`). The
session id, user id, access token and refresh token are stored in `localStorage`; the profile page reads
and updates `/user-service/api/v1/users/{user id}`. When the gateway rejects the
access token (it expires after an hour), `src/api/client.js` gets new tokens from
`POST /user-service/api/v1/sessions/{id}/tokens` and repeats the request. Requests that fail at the same moment share
one refresh, since each refresh token works only once. If the refresh token is rejected too, the tokens
are removed and the user is sent to the sign-in page. Signing out deletes the session
(`DELETE /user-service/api/v1/sessions/{id}`).
