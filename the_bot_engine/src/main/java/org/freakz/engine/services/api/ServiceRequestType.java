package org.freakz.engine.services.api;

public enum ServiceRequestType {
  AiService,
  HermesAiService,
  AiRoutesStatus,


  BotInfoQuery,
  BuildInfoQuery,
  ConnectionActivityService,
  ConnectionControlService,
  SendAlertToNotifyChannels,

  DataSaverList,

  ForecaWeatherService,
  CmpWeatherService,

  WeatherAPIService,

  GetDataValuesService,

  GetTopCountsService,
  GetTopStatsRequest,
  GenerateGluggaCountsPage,
  GenerateGluggaWeekdayPage,
  GenerateGluggaStreakPage,

  ChannelOpRequest,

  SendMessageByEchoToAlias,

  TranslateService,

  TestService1,

  TestService2,

  TestService3,

  SetEnv,

  UnSetEnv,

  ListEnv,
}
