# Scala/Play Framework demo of idQ Authentication

This repository demonstrates how to integrate idQ authentication into your Scala / Play Framework web application.

## Prerequisite
1. You need to have an idQ Developer account
2. Login to your Account Portal and issue OAuth2 credentials for the demo app See <https://docs.idquanta.com> for instructions on issuing OAuth2 credentials.
3. Specify http://localhost:9000/redirect as the Callback URL when creating your OAuth2 credentials.


## Usage
1. Clone this repository
2. Configure your OAuth2 Credentials in `conf/application.conf`

```
oauth.client_id = "YOUR_CLIENT_ID"
oauth.client_secret = "YOUR_CLIENT_SECRET"
oauth.redirect_url = "YOUR_REDIRECT_URI"
oauth.base_url = "idQ Trust as a Service OAuth 2.0 endpoint, eg. https://beta.idquanta.com/idqoauth"
```

3. Execute `sbt run`
4. Open <http://localhost:9000>
