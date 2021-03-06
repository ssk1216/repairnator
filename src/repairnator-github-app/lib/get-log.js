const request = require('request-promise-native')

/**
 * Retrieves the Travis log from the Travis job API endpoint
 * https://developer.travis-ci.com/resource/job
 * @module lib/get-log.js
 * @param {array} jobs - An array of jobs
 * @param {boolean} isOrg - Identify whether it is for travis-ci.org
 */

module.exports = (jobs, isOrg) => {
  // If not passed a job ID, throw an error
  if (!jobs[0].id) {
    throw new Error(`No job ID found`)
  }

  // For now, we're only going to retrieve the log from the first job
  // It is possible that there are multiple jobs that run against a build
  const jobId = jobs[0].id

  // build the request URL
  const requestUrl = `https://api.travis-ci.` + (isOrg ? `org` : `com`) + `/v3/job/${jobId}/log`

  // Set the parameters for the request to the Travis API
  // https://developer.travis-ci.com/gettingstarted
  const options = {
    uri: requestUrl,
    headers: {
      'Travis-API-Version': 3,
      Authorization: 'token ' + (isOrg ? process.env.TRAVIS_ORG : process.env.TRAVIS_COM)
    },
    json: true
  }

  // Make the request to the Travis API
  // On success return the raw Travis log content
  return request(options)
    .then(function (log) {
      return log.content
    })
    .catch(function (err) {
      throw new Error(`Error requesting Travis API ${err}`)
    })
}