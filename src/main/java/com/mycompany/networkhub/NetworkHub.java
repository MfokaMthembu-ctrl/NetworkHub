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

        input.close();
    }
}
