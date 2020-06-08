package com.example.demo.aspects;
import com.example.demo.classes.Campaign;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Pointcut(value = "execution(public * com.example.demo.*.*.*(..))")
    public void genericMethodCallPointcut(){}

    @Pointcut(value = "execution(public * com.example.demo.services.CampaignServiceImpl.insertCampaign(..))")
    public void insertCampaignPointcut(){ }

    @Pointcut(value = "execution(public * com.example.demo.services.CampaignServiceImpl.getAllCampaigns(..))")
    public void getAllCampaignsPointcut(){}

    @Pointcut(value = "execution(public * com.example.demo.controllers.HomeController.index(..))")
    public void welcomeHomePagePointcut(){}

    @Before("genericMethodCallPointcut()")
    public void loggerGenericMethodCall(JoinPoint jp) {
        String methodName = jp.getSignature().getName();
        String className = jp.getTarget().getClass().toString();
        Object[] args = jp.getArgs();
        log.info("method invoked " + className + " : " + methodName + "()" + " with {} arguments", args.length);
    }

    @After("insertCampaignPointcut()")
    public void loggerInsertCampaign(JoinPoint jp) {
        Campaign campaignArg = (Campaign) jp.getArgs()[0];
        log.info("Campaign {} has been inserted", campaignArg.getTitle());
    }

    @Around("getAllCampaignsPointcut()")
    public Object loggerGetAllCampaigns(ProceedingJoinPoint pjp) throws Throwable {
        List<Campaign> response = (ArrayList) pjp.proceed();
        log.info("{} campaigns were found ", response.size());
        return response;
    }

    @Before("welcomeHomePagePointcut()")
    public void loggerHomePage(){
        log.info("Welcome to home page!");
    }
}
