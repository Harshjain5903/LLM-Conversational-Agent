/**
 * Lambda Handler for LLM Conversational Agent
 * Proxies requests to EC2 instance
 * Author: Harsh Jain
 */

const https = require('https');
const http = require('http');

// Configuration
const AGENT_ENDPOINT = process.env.AGENT_ENDPOINT || 'http://llm-agent:8080';
const TIMEOUT = 60000; // 60 seconds

/**
 * Make HTTP request to agent
 */
async function invokeAgent(method, path, body = null) {
  return new Promise((resolve, reject) => {
    try {
      const url = new URL(path, AGENT_ENDPOINT);
      const protocol = url.protocol === 'https:' ? https : http;
      
      const options = {
        method: method,
        headers: {
          'Content-Type': 'application/json',
        },
        timeout: TIMEOUT,
      };

      const req = protocol.request(url, options, (res) => {
        let data = '';

        res.on('data', (chunk) => {
          data += chunk;
        });

        res.on('end', () => {
          try {
            const parsed = JSON.parse(data);
            resolve({
              statusCode: res.statusCode,
              body: parsed,
              headers: res.headers,
            });
          } catch (e) {
            resolve({
              statusCode: res.statusCode,
              body: data,
              headers: res.headers,
            });
          }
        });
      });

      req.on('error', (error) => {
        console.error('Request error:', error);
        reject({
          statusCode: 503,
          body: {
            error: 'Service unavailable',
            message: error.message,
          },
        });
      });

      req.on('timeout', () => {
        req.destroy();
        reject({
          statusCode: 504,
          body: {
            error: 'Gateway timeout',
            message: 'Request to agent timed out',
          },
        });
      });

      if (body) {
        req.write(JSON.stringify(body));
      }

      req.end();
    } catch (error) {
      console.error('Error:', error);
      reject({
        statusCode: 500,
        body: {
          error: 'Internal server error',
          message: error.message,
        },
      });
    }
  });
}

/**
 * Lambda handler
 */
exports.handler = async (event) => {
  console.log('Event:', JSON.stringify(event, null, 2));

  try {
    const method = event.requestContext.http.method;
    const path = event.rawPath;
    const body = event.body ? JSON.parse(event.body) : null;

    console.log(`${method} ${path}`);

    const result = await invokeAgent(method, path, body);

    return {
      statusCode: result.statusCode,
      headers: {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*',
      },
      body: JSON.stringify(result.body),
    };
  } catch (error) {
    console.error('Error:', error);

    return {
      statusCode: error.statusCode || 500,
      headers: {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*',
      },
      body: JSON.stringify(error.body || {
        error: 'Internal server error',
        message: error.message,
      }),
    };
  }
};
