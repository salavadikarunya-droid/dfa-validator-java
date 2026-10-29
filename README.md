# DFA Validator (Java)

A simple web page, powered by a small Java server, that validates Indian mobile numbers and Aadhaar numbers using Deterministic Finite Automata (DFA). It shows whether each input is valid, the path of states the DFA followed, and the exact reason for rejection.

Built as a mini project for **Automata Theory & Compiler Design**.

## Features
- Two DFAs: Indian mobile number and Aadhaar number
- Shows the state path (for example `q0 -> q1 -> q2 ...`)
- Explains the rejection point (position, current state, expected input)
- Built with plain Java (no frameworks or external libraries)
- Works on a phone through your PC's IP on the same Wi-Fi

## How it works
A DFA is defined as a 5-tuple **(Q, Σ, δ, q0, F)**: states, alphabet, transition function, start state and final states. The program reads the input one character at a time and follows the transition table. If no transition exists, the input is rejected. If the input ends in a final state, it is accepted.

### Phone DFA
- Σ = digits 0-9 and `+`
- Format: optional `+91`, then 10 digits, with the first digit 6-9
- Start state: `q0`, final state: `q13`

| Input | Result |
|---|---|
| `+919542594152` | Valid |
| `9542594152` | Valid |
| `5542594152` | Invalid (starts with 5) |
| `95425941` | Invalid (too short) |

### Aadhaar DFA
- Σ = digits 0-9 and space
- Format: 12 digits, first digit 2-9, optional single space after the 4th and 8th digit
- Start state: `q0`, final state: `q14`

| Input | Result |
|---|---|
| `2345 5689 9853` | Valid |
| `234556899853` | Valid |
| `1345 5689 9853` | Invalid (starts with 1) |
| `2345 5689 985` | Invalid (too short) |

## Project structure
```
dfa-validator-java/
├── DfaWeb.java
├── .gitignore
└── README.md
```

## Requirements
- JDK 17 or later

## Run
```
javac DfaWeb.java
java DfaWeb
```
Then open http://127.0.0.1:5000 in your browser. Press `Ctrl + C` in the terminal to stop the server.

To open it from a phone, use your PC's IP address and port 5000 on the same Wi-Fi.

## Notes
Use dummy Aadhaar numbers only. This project checks the format, not whether a number is genuinely issued.

## Possible improvements
- Verhoeff checksum validation for Aadhaar
- Diagram of the DFA with the path highlighted
- More validators (email, PAN, IFSC)
