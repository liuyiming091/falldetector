package application.cepheus.falldetector.service;

import application.cepheus.falldetector.data.Channel;

/**
 * Created by liuyiming091 on 2016/10/16 0016.
 */
public interface WeatherServiceCallback {
    void serviceSuccess(Channel channel);
    void serviceFailure(Exception exception);
}
