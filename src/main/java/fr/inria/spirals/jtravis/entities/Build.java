package fr.inria.spirals.jtravis.entities;

import fr.inria.spirals.jtravis.helpers.RepositoryHelper;
import fr.inria.spirals.jtravis.pojos.BuildPojo;

import java.util.Date;
import java.util.List;

/**
 * Created by urli on 21/12/2016.
 */
public class Build extends BuildPojo {
    private Repository repository;
    private Commit commit;
    private BuildConfig config;
    private List<Job> jobs;

    public BuildStatus getBuildStatus() {
        if (this.getState() != null) {
            return BuildStatus.valueOf(this.getState().toUpperCase());
        } else {
            return null;
        }
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Repository getRepository() {
        if (repository == null) {
            if (this.getRepositoryId() != 0) {
                this.repository = RepositoryHelper.getRepositoryFromId(this.getRepositoryId());
            }
        }
        return repository;
    }

    public void setCommit(Commit commit) {
        this.commit = commit;
    }

    public Commit getCommit() {
        return commit;
    }

    public List<Job> getJobs() {
        return jobs;
    }

    public BuildConfig getConfig() {
        return config;
    }

    public void setConfig(BuildConfig config) {
        this.config = config;
    }

    @Override
    public String toString() {
        return "Build{" +
                super.toString()+
                "commit=" + commit +
                "config=" + config +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Build build = (Build) o;

        if (repository != null ? !repository.equals(build.repository) : build.repository != null) return false;
        if (commit != null ? !commit.equals(build.commit) : build.commit != null) return false;
        if (config != null ? !config.equals(build.config) : build.config != null) return false;
        return jobs != null ? jobs.equals(build.jobs) : build.jobs == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (repository != null ? repository.hashCode() : 0);
        result = 31 * result + (commit != null ? commit.hashCode() : 0);
        result = 31 * result + (config != null ? config.hashCode() : 0);
        result = 31 * result + (jobs != null ? jobs.hashCode() : 0);
        return result;
    }
}
