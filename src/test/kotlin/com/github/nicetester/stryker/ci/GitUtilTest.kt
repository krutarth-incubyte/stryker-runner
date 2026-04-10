package com.github.nicetester.stryker.ci

import junit.framework.TestCase

class GitUtilTest : TestCase() {

    fun testParseOwnerRepoFromSshUrl() {
        val result = GitUtil.parseOwnerRepo("git@github.com:krutarth-incubyte/stryker-runner.git")
        assertEquals("krutarth-incubyte/stryker-runner", result)
    }

    fun testParseOwnerRepoFromSshUrlWithoutGitSuffix() {
        val result = GitUtil.parseOwnerRepo("git@github.com:owner/repo")
        assertEquals("owner/repo", result)
    }

    fun testParseOwnerRepoFromHttpsUrl() {
        val result = GitUtil.parseOwnerRepo("https://github.com/krutarth-incubyte/stryker-runner.git")
        assertEquals("krutarth-incubyte/stryker-runner", result)
    }

    fun testParseOwnerRepoFromHttpsUrlWithoutGitSuffix() {
        val result = GitUtil.parseOwnerRepo("https://github.com/owner/repo")
        assertEquals("owner/repo", result)
    }

    fun testParseOwnerRepoFromOrgAliasSshUrl() {
        val result = GitUtil.parseOwnerRepo("org-7375850@github.com:healthsparq/listings-management.git")
        assertEquals("healthsparq/listings-management", result)
    }

    fun testParseOwnerRepoFromOrgAliasSshUrlWithoutGitSuffix() {
        val result = GitUtil.parseOwnerRepo("org-12345@github.com:myorg/myrepo")
        assertEquals("myorg/myrepo", result)
    }

    fun testParseOwnerRepoReturnsNullForNonGithubUrl() {
        val result = GitUtil.parseOwnerRepo("git@gitlab.com:owner/repo.git")
        assertNull(result)
    }

    fun testParseOwnerRepoReturnsNullForInvalidUrl() {
        val result = GitUtil.parseOwnerRepo("not-a-url")
        assertNull(result)
    }

    fun testParseOwnerRepoHandlesWhitespace() {
        val result = GitUtil.parseOwnerRepo("  git@github.com:owner/repo.git  \n")
        assertEquals("owner/repo", result)
    }
}
