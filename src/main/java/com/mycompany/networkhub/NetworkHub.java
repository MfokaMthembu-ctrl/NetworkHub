/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.networkhub;

import java.util.Scanner;

/**
 * Driver class for the application. Covers two parts of the brief:
 *
 *   Part 1 - registering an account and logging in.
 *   Part 2 - once logged in, the QuickChat menu that lets the user
 *            compose, send, disregard or store messages.
 *
 * @author Student
 */
public class NetworkHub {

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        // =====================================================
        //  PART 1 - REGISTRATION AND LOGIN
        // =====================================================

        System.out.println("=== Register a new account ===");

        System.out.print("Please enter your first name: ");
        String firstName = input.nextLine();

        System.out.print("Please enter your last name: ");
        String lastName = input.nextLine();

        Login login;
        boolean registered = false;

        // Keep asking until every registration condition is met.
        do {
            System.out.print("Please enter your username: ");
            String username = input.nextLine();

            System.out.print("Please enter your password: ");
            String password = input.nextLine();

            System.out.print("Please enter your cell phone number "
                    + "(including the international code, e.g. +27838968976): ");
            String cellPhoneNumber = input.nextLine();

            login = new Login(firstName, lastName, username, password,
                    cellPhoneNumber);

            // Feedback on each individual field
            System.out.println(login.returnUsernameMessage());
            System.out.println(login.returnPasswordMessage());
            System.out.println(login.returnCellPhoneMessage());

            String registrationMessage = login.registerUser();
            System.out.println(registrationMessage);

            registered = login.checkUserName()
                    && login.checkPasswordComplexity()
                    && login.checkCellPhoneNumber();

            if (!registered) {
                System.out.println("Please try registering again.");
            }

        } while (!registered);

        System.out.println("=== Log in to your account ===");

        System.out.print("Please enter your username: ");
        String loginUsername = input.nextLine();

        System.out.print("Please enter your password: ");
        String loginPassword = input.nextLine();

        boolean loginSuccessful = login.loginUser(loginUsername, loginPassword);
        System.out.println(login.returnLoginStatus(loginSuccessful));

        // Point 1 of Part 2: users may only send messages if they have
        // logged in successfully, so a failed login stops the program here.
        if (!loginSuccessful) {
            System.out.println("Login failed - closing the application.");
            input.close();
            return;
        }

        // =====================================================
        //  PART 2 - THE QUICKCHAT MENU
        // =====================================================

        System.out.println();
        System.out.println("Welcome to QuickChat.");

        // Point 5 of the brief: the user decides how many messages they
        // want to enter when the application starts, and may then only
        // enter that many. This is asked ONCE, here, not every time the
        // "Send Messages" option is chosen.
        int messageLimit = readPositiveInteger(input,
                "How many messages would you like to enter? ");

        // Keeps track of how many of those messages have been entered so far.
        int messagesEntered = 0;

        // "running" controls the menu loop. It only becomes false when the
        // user chooses Option 3 (Quit) - point 4 of the brief.
        boolean running = true;

        while (running) {
            System.out.println();
            System.out.println("Please choose an option:");
            System.out.println("1) Send Messages");
            System.out.println("2) Show recently sent messages");
            System.out.println("3) Quit");
            System.out.print("Enter your choice: ");

            int menuChoice = readMenuChoice(input);

            switch (menuChoice) {
                case 1:
                    if (messagesEntered >= messageLimit) {
                        System.out.println("You have already entered all "
                                + messageLimit + " of your messages.");
                    } else {
                        messagesEntered += handleSendMessages(input,
                                messagesEntered, messageLimit);

                        // Brief: the total is displayed once all the
                        // messages have been sent.
                        System.out.println();
                        System.out.println("Total messages sent: "
                                + Message.returnTotalMessagess());
                    }
                    break;

                case 2:
                    // Still in development, so the brief asks for a
                    // simple placeholder message.
                    System.out.println("Coming Soon.");
                    break;

                case 3:
                    running = false;
                    System.out.println("Total messages sent: "
                            + Message.returnTotalMessagess());
                    System.out.println("Thank you for using QuickChat. Goodbye!");
                    break;

                default:
                    System.out.println("That isn't a valid option; please enter 1, 2 or 3.");
                    break;
            }
        }

        input.close();
    }

    /**
     * Safely reads the user's menu choice as an integer. If they type
     * something that isn't a whole number, this returns -1 instead of
     * crashing with a NumberFormatException; the caller treats -1 as
     * "not a valid option" and simply asks again.
     */
    private static int readMenuChoice(Scanner input) {
        String rawInput = input.nextLine().trim();
        try {
            return Integer.parseInt(rawInput);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Walks the user through every message they still have left to enter.
     *
     * @param alreadyEntered how many messages were entered earlier
     * @param messageLimit   the total the user said they wanted to enter
     * @return how many messages were entered during this call
     */
    private static int handleSendMessages(Scanner input, int alreadyEntered,
                                          int messageLimit) {
        int enteredThisRound = 0;

        for (int number = alreadyEntered + 1; number <= messageLimit; number++) {
            System.out.println();
            System.out.println("--- Message " + number + " of " + messageLimit + " ---");

            Message message = composeValidMessage(input);

            // Confirms the randomly generated ID, as the brief requires.
            System.out.println(message.returnMessageIDMessage());

            int actionChoice = readSendChoice(input);
            System.out.println(message.sentMessage(actionChoice));

            // Point 7 of the brief: show the full details once a message
            // has been SENT, in the order ID, Hash, Recipient, Message.
            if (actionChoice == 1) {
                printMessageDetails(message);
            }

            enteredThisRound++;
        }

        return enteredThisRound;
    }

    /**
     * Repeatedly asks for a recipient number and message body until
     * both pass validation, printing the system's exact success or
     * failure message each time so the user knows what to fix.
     * Returns a Message that is guaranteed to be valid.
     */
    private static Message composeValidMessage(Scanner input) {
        String recipient;
        Message candidateMessage;

        // Keep asking until checkRecipientCell() is happy. An empty body
        // is used only so a Message exists to run the check on.
        while (true) {
            System.out.print("Enter the recipient's cell number (with international code): ");
            recipient = input.nextLine().trim();

            candidateMessage = new Message(recipient, "");
            String recipientResult = candidateMessage.checkRecipientCell();
            System.out.println(recipientResult);

            if (recipientResult.equals("Cell phone number successfully captured.")) {
                break;
            }
        }

        // Now keep asking for the text until checkMessageLength() accepts it.
        while (true) {
            System.out.print("Enter your message (250 characters or fewer): ");
            String messageText = input.nextLine();

            candidateMessage = new Message(recipient, messageText);
            String lengthResult = candidateMessage.checkMessageLength();
            System.out.println(lengthResult);

            if (lengthResult.equals("Message ready to send.")) {
                break;
            }
        }

        return candidateMessage;
    }

    /**
     * Asks what to do with the composed message, in the order the brief
     * lists the options: 1 = Send, 2 = Disregard, 3 = Store.
     * Keeps asking until the user enters 1, 2 or 3.
     */
    private static int readSendChoice(Scanner input) {
        while (true) {
            System.out.println("What would you like to do with this message?");
            System.out.println("1) Send Message");
            System.out.println("2) Disregard Message");
            System.out.println("3) Store Message to send later");
            System.out.print("Enter your choice: ");

            int choice = readMenuChoice(input);
            if (choice == 1 || choice == 2 || choice == 3) {
                return choice;
            }
            System.out.println("That isn't a valid option; please enter 1, 2 or 3.");
        }
    }

    /**
     * Keeps prompting until the user enters a whole number greater than
     * zero, so the rest of the program never sees a zero or negative count.
     */
    private static int readPositiveInteger(Scanner input, String prompt) {
        while (true) {
            System.out.print(prompt);
            String rawInput = input.nextLine().trim();
            try {
                int value = Integer.parseInt(rawInput);
                if (value > 0) {
                    return value;
                }
                System.out.println("Please enter a number greater than zero.");
            } catch (NumberFormatException e) {
                System.out.println("That isn't a valid number; please try again.");
            }
        }
    }

    /**
     * Prints a sent message's full details in the order the brief
     * specifies: Message ID, Message Hash, Recipient, Message.
     */
    private static void printMessageDetails(Message message) {
        System.out.println("Message ID: " + message.getMessageID());
        System.out.println("Message Hash: " + message.getMessageHash());
        System.out.println("Recipient: " + message.getRecipient());
        System.out.println("Message: " + message.getMessageText());
    }
}