package se.kry.codetest.domain;

import java.util.Objects;

public class ServiceDetail {

    private String serviceName;
    private String url;
    private String addedOn;

    public ServiceDetail(String serviceName, String url, String addedOn) {
        this.serviceName = serviceName;
        this.url = url;
        this.addedOn = addedOn;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getUrl() {
        return url;
    }

    public String getAddedOn() {
        return addedOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceDetail that = (ServiceDetail) o;
        return url.equals(that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }
}
