// Import AWS SDK to interact with AWS DynamoDB
const AWS = require('aws-sdk');

// Import UUID to generate unique user IDs
const { v4: uuidv4 } = require('uuid');

// Initialize a DynamoDB DocumentClient instance
const dynamoDB = new AWS.DynamoDB.DocumentClient();

// Lambda entry point
exports.handler = async (event) => {
  console.log('Processing create user request');
  
  try {
    // Parse incoming JSON request body
    const body = JSON.parse(event.body);

    // Construct user object with unique ID and timestamps
    const user = {
      userId: uuidv4(),                   // unique identifier
      name: body.name,                    // user name from request
      email: body.email,                  // user email from request
      createdAt: new Date().toISOString() // record creation time
    };

    // Save user record to DynamoDB table
    await dynamoDB.put({
      TableName: process.env.USERS_TABLE, // table name injected via environment variable
      Item: user
    }).promise();

    console.log('User created successfully:', user.userId);

    // Return success response with created user data
    return {
      statusCode: 201, // HTTP Created
      headers: {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*' // Allow all CORS origins
      },
      body: JSON.stringify(user)
    };
  } catch (error) {
    // Log and return an error response
    console.error('Error creating user:', error);
    return {
      statusCode: 500, // Internal Server Error
      headers: {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*'
      },
      body: JSON.stringify({ error: error.message })
    };
  }
};
