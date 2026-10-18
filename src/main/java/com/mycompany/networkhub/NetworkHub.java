/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.networkhub;

import java.util.Scanner;

/**
 *
 * @author Student
 */

public class NetworkHub {

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        // =====================================================
        //  PART 1 - REGISTRATION AND LOGIN (unchanged)
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

        // The brief requires that only a user who has logged in
        // successfully can reach the messaging menu, so an unsuccessful
        // login stops the program here rather than falling through.
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

        // "running" controls the outer menu loop. It only becomes false
        // once the user explicitly chooses Option 3 (Quit), as point 4
        // of the brief requires - the application should keep running
        // until the user asks to leave.
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
                    handleSendMessages(input);
                    break;

                case 2:
                    // This feature is still in development, so the
                    // brief asks for a simple placeholder message
                    // rather than an actual implementation here.
                    System.out.println("Coming Soon.");
                    break;

                case 3:
                    running = false;
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
     * something that isn't a whole number (e.g. accidentally hit enter,
     * or typed a letter), this returns -1 instead of crashing the
     * program with a NumberFormatException - the calling code treats
     * -1 as "not a valid option" and simply re-shows the menu.
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
     * Runs the full "Send Messages" flow: asks how many messages the
     * user wants to compose this round (point 5 of the brief), then
     * walks through composing, validating, and acting on each one in
     * turn. Once every message in the batch has been dealt with, it
     * prints the running total of messages sent so far (point 6).
     */
    private static void handleSendMessages(Scanner input) {
        int numberOfMessages = readPositiveInteger(input,
                "How many messages would you like to enter? ");

        for (int i = 1; i <= numberOfMessages; i++) {
            System.out.println();
            System.out.println("--- Message " + i + " of " + numberOfMessages + " ---");

            Message message = composeValidMessage(input);

            int actionChoice = readSendChoice(input);
            String resultMessage = message.sentMessage(actionChoice);
            System.out.println(resultMessage);

            // The brief asks for the full details of a message to be
            // shown on screen once it has actually been sent (not
            // stored or disregarded), in the order: ID, Hash, Recipient,
            // Message.
            if (actionChoice == 1) {
                printMessageDetails(message);
            }
        }

        // Once this whole batch has been processed, show the running
        // total of everything sent so far in the program's lifetime.
        System.out.println();
        System.out.println("Total messages sent so far: " + Message.returnTotalMessagess());
    }

    /**
     * Repeatedly asks for a recipient number and message body until
     * both pass validation, printing the system's exact success or
     * failure message at each attempt so the user knows what to fix.
     * Returns a Message object that is guaranteed to be valid.
     */
    private static Message composeValidMessage(Scanner input) {
        String recipient;
        Message candidateMessage;

        // Keep asking for a recipient number until checkRecipientCell()
        // is happy with it. A placeholder message body is used here
        // purely so a Message object can be constructed to run the
        // check; the real body is collected afterwards.
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

        // Now that the recipient is valid, keep asking for message text
        // until checkMessageLength() accepts it.
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
     * Asks the user what to do with a fully composed message and keeps
     * asking until they enter 1, 2 or 3.
     */
    private static int readSendChoice(Scanner input) {
        while (true) {
            System.out.println("What would you like to do with this message?");
            System.out.println("1) Send Message");
            System.out.println("2) Store Message to send later");
            System.out.println("3) Disregard Message");
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
     * zero, so the rest of the program never has to deal with a
     * negative or zero message count.
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
     * Prints a sent message's full details in the exact order the
     * brief specifies: Message ID, Message Hash, Recipient, Message.
     */
    private static void printMessageDetails(Message message) {
        System.out.println("Message ID: " + message.getMessageID());
        System.out.println("Message Hash: " + message.getMessageHash());
        System.out.println("Recipient: " + message.getRecipient());
        System.out.println("Message: " + message.getMessageText());
    }
}