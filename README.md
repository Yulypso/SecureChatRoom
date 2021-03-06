# secureApp (JavaCard - Smartcard)

Secure ChatRoom RSA Encryption Challenge/Response Authentication by SmartCard insertion, DES exchange encryption

## Author

[![Linkedin: Thierry Khamphousone](https://img.shields.io/badge/-Thierry_Khamphousone-blue?style=flat-square&logo=Linkedin&logoColor=white&link=https://www.linkedin.com/in/tkhamphousone/)](https://www.linkedin.com/in/tkhamphousone)

---

<br/>

## Setup

```bash
$ git clone https://github.com/Yulypso/SecureApp.git
```

---

## Required misc


<p align="center" width="100%">
    <img align="center" src="https://user-images.githubusercontent.com/59794336/160142671-644e380f-57f5-4822-ab57-1b5bf4eca443.png"/>
</p>

<br/>

**Required JDK:** 

- jdk-17.0.2
- jdk1.6


<br/>

---

## Run batch files (Windows only)

Run these scripts from 1 to 7 in order to start the secureApp. 

```sh
> 1_makeApplet.bat
> 2_card-deleteApplet.bat
> 3_card-installApplet.bat
> 4_makeServer.bat
> 5_run_server.bat
> 6_makeClient.bat
> 7_runClient.bat
```

**Server default port is: 7777**

<br/>

---

## Features

### Registration 

Insert Smartcard to get registered.

<p align="center" width="100%">
    <img align="center" src="https://user-images.githubusercontent.com/59794336/155338014-7276db3d-9fe9-4f6b-8e9b-af2f163be4da.png"/>
</p>

<br/>

---

### Login and List connected users

Insert the Smartcard to connect to the server.

<p align="center" width="100%">
    <img align="center" src="https://user-images.githubusercontent.com/59794336/155338840-b6c929a4-d719-4b76-81f4-a7d466359760.png"/>
</p>

<br/>

---

### Send encrypted messages

1) Messages are encrypted by the Smartcard before being sent to the server. 
2) Users retrieve and decrypt the encrypted message using their Smartcard.

<p align="center" width="100%">
    <img align="center" src="https://user-images.githubusercontent.com/59794336/155339571-87aca852-554d-4baf-bc53-2ada6fa72f5f.png"/>
</p>

<br/>

---

### Send files

<p align="center" width="100%">
    <img align="center" src="https://user-images.githubusercontent.com/59794336/155341154-3332527f-46cb-4d63-909b-dba48f779a0e.png"/>
</p>

<br/>

---

### Other commands

- Private message: /msg <user> <msg>
- Exit: /exit or /logout
- List: /list

<br/>

---

### Administrator

- Kill all connected users: /killall
- Kill a specific connected user: /kill <user>
- Halt server: /halt
- Save registered users into a database: /savebdd <database_name>
- Load registered users from the database: /loadbdd <database_name>