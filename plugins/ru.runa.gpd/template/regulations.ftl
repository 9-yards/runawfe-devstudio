<html>
	<head>
		<title> ${proc.getName()} </title>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />

	</head>
	<body>
		<style>
			.definition-header-1 { font-weight: 700;
								   font-family: Verdana, Geneva, sans-serif;
								   font-size: 2.5em; 
								   text-align: center;}
			.definition-description-text-1 { font-weight: 700; }
			.node-type-text-1 { font-weight: 700; }
			.node-swimlane-text-1 { font-weight: 700; }
			.node-description-text-1 { font-weight: 700; }
			.subprocess-name-text-1 { font-weight: 700; }
			.subprocess-node-header-text {	font-family: Geneva, Arial, Helvetica, monospace;
											font-weight: 400;
											text-align: center;}
			.subprocess-node-header-td {background-color: #e3f4ff;}
			.node-validation-text-1 { font-weight: 700; }
		</style>
		<h1 class="definition-header-1">Регламент выполнения бизнес-процесса </h1>
		<h1 class="definition-header-1">${proc.getName()}</h1>
		
		<table width="80%" border="0" align="center">
		
		<tr bgcolor="#e3f4ff"><td><span class="definition-description-text-1">Общее описание бизнес-процесса</span></td></tr>
		
		<tr><td> 
		
		<#if proc.getDescription() ?? >
		
		<br/>
		<p><strong>  Краткое описание бизнес-процесса: ${proc.getDescription()} </strong> </p>
		</#if>
		
		<#if brief ?? >
			<p> <strong> Подробное описание бизнес-процесса: </strong> </p>
			${brief}
		</#if>
		
		<#-- SWIMLANES -->
		<p> <strong> Список ролей бизнес-процесса: </strong> </p>
		<ul>
		<#list proc.getSwimlanes() as swimlane >
			<li> ${swimlane.getName()} </li>
		</#list>
		</ul>
		<br>
		
		<#-- VARIABLES -->
		<p> <strong> Cписок переменных бизнес-процесса: </strong> </p>
		<ul>
			<#list proc.getVariables(false,false,null) as var>
				<li> ${var.getName()} </li>
			</#list>
		</ul>
		
		<br/>
		</td>
		</tr>
		
		<tr bgcolor="#e3f4ff"><td><p><strong> Описание действий бизнес-процесса: </strong></p></td></tr>
		
		<#-- NODES -->
		<#-- proc.getChildren(model.node) as node-->
		<#list listOfNodes as node>
			<#if node.getNodeRegulationsProperties().getIsEnabled() == true>
				<#-- START POINT -->
				<#if node.class.simpleName == "StartState">
					<tr>
					<#if node.getParent().getName() != proc.getName()>
						<tr><td  class="subprocess-node-header-td"><div class="subprocess-node-header-text">Действие в рамках подпроцесса "${node.getParent().getName()}"</div></td></tr>
					</#if>
					<td><br/><p> Начало выполнения бизнес-процесса: ${node.getName()} <br/>
					<#if node.getSwimlane() ?? >
						 <span class="node-swimlane-text-1">Роль:</span> ${node.getSwimlane().getName()} <br/>
					</#if>
					<#if node.getNodeRegulationsProperties().getDescriptionForUser()?length != 0 >
						<span class="node-description-text-1">Описание:</span> <br/>
						${node.getNodeRegulationsProperties().getDescriptionForUser()?replace("\n","<br />")}
						<br/><br/>
					</#if>
					
                    <#if node.hasFormValidation() == true >
                       <#assign formNodeValidation = mapOfFormNodeValidation[node.getId()] >
                        <span class="node-validation-text-1">Валидация:</span> <br/>
                        <ul>
                         <#assign formNodeValidationGetFieldConfigs = formNodeValidation.getFieldConfigs()>
                            <#list formNodeValidationGetFieldConfigs?keys as variableName>
                            <li>Переменная &quot;${variableName}&quot;
                                <#list formNodeValidationGetFieldConfigs[variableName]?keys as nodeFieldConfigsValueKey> 
                                    <ul>
                                       <li>Тип валидатора: &quot;${formNodeValidationGetFieldConfigs[variableName][nodeFieldConfigsValueKey].getType()}&quot;</li>
                                       <#if formNodeValidationGetFieldConfigs[variableName][nodeFieldConfigsValueKey].getMessage()?length != 0>
                                       <li>Сообщение валидатора: &quot;${formNodeValidationGetFieldConfigs[variableName][nodeFieldConfigsValueKey].getMessage()}&quot;</li>
                                       </#if>
                                       <#if formNodeValidationGetFieldConfigs[variableName][nodeFieldConfigsValueKey].getParams()?size != 0>
                                           <li>Параметры валидатора:
                                               <ul>
                                               <#list formNodeValidationGetFieldConfigs[variableName][nodeFieldConfigsValueKey].getParams()?keys as parameterName>
                                               <li>${parameterName} = &quot;${formNodeValidationGetFieldConfigs[variableName][nodeFieldConfigsValueKey].getParams()[parameterName]}&quot;</li>
                                               </#list>
                                               </ul>
                                           </li> 
                                       </#if>
                                     </ul>
                                     </li>
                                     <br />
                               </#list>
                            </#list> 
                        </ul>
                        <br/>
                    </#if>
					
					<#assign afterStart = node.getLeavingTransitions()?first >
					Далее управление переходит к шагу <a href="#${afterStart.getTarget().getId()}">${afterStart.getTarget().getName()}</a></p>
					<br/></td>
					</tr>
				</#if>
			
				<#-- TaskState -->
				<#if node.class.simpleName == "TaskState" || node.class.simpleName == "Decision"  || node.class.simpleName == "Conjunction">
					<tr>
					<#if node.getParent().getName() != proc.getName()>
						<tr><td  class="subprocess-node-header-td"><div class="subprocess-node-header-text">Действие в рамках подпроцесса "${node.getParent().getName()}"</div></td></tr>
					</#if>
					<td>
					<hr color="#e3f4ff">
					
					<#-- name -->
					<p id="${node.getId()}"> <font color="#005D85" size="3" face="Verdana, Geneva, sans-serif"> <strong> Шаг: ${node.getName()} </strong> </font> </p>
					
					<#-- type -->
					<p>
					<#if node.class.simpleName == "Conjunction">
						<span class="node-type-text-1">Тип шага:</span> Соединение<br/>
					<#elseif node.getLeavingTransitions()?size == 1>
						<span class="node-type-text-1">Тип шага:</span> Действие<br/>
					<#else>
						<span class="node-type-text-1">Тип шага:</span> Ветвление<br/>
					</#if>
					
					<#-- swimlane -->
					<#if node.class.simpleName == "TaskState" && node.getSwimlane() ?? >
						<span class="node-swimlane-text-1">Роль:</span> ${node.getSwimlane().getName()}<br/>
					</#if>
					
					<#if node.getNodeRegulationsProperties().getDescriptionForUser()?length != 0 >
						<span class="node-description-text-1">Описание:</span> <br/>
						${node.getNodeRegulationsProperties().getDescriptionForUser()?replace("\n","<br />")}
						<br/><br/>
					</#if>
					
                    <#if node.hasFormValidation() == true >
                       <#assign formNodeValidation = mapOfFormNodeValidation[node.getId()] >
                        <span class="node-validation-text-1">Валидация:</span> <br/>
                        <ul>
                         <#assign formNodeValidationGetFieldConfigs = formNodeValidation.getFieldConfigs()>
                            <#list formNodeValidationGetFieldConfigs?keys as variableName>
                            <li>Переменная &quot;${variableName}&quot;
                                <#list formNodeValidationGetFieldConfigs[variableName]?keys as nodeFieldConfigsValueKey>
                                    <ul>
                                       <li>Тип валидатора: &quot;${formNodeValidationGetFieldConfigs[variableName][nodeFieldConfigsValueKey].getType()}&quot;</li>
                                       <#if formNodeValidationGetFieldConfigs[variableName][nodeFieldConfigsValueKey].getMessage()?length != 0>
                                       <li>Сообщение валидатора: &quot;${formNodeValidationGetFieldConfigs[variableName][nodeFieldConfigsValueKey].getMessage()}&quot;</li>
                                       </#if>
                                       <#if formNodeValidationGetFieldConfigs[variableName][nodeFieldConfigsValueKey].getParams()?size != 0>
                                           <li>Параметры валидатора:
                                               <ul>
                                               <#list formNodeValidationGetFieldConfigs[variableName][nodeFieldConfigsValueKey].getParams()?keys as parameterName>
                                               <li>${parameterName} = &quot;${formNodeValidationGetFieldConfigs[variableName][nodeFieldConfigsValueKey].getParams()[parameterName]}&quot;</li>
                                               </#list>
                                               </ul>
                                           </li> 
                                       </#if>
                                     </ul>
                                     </li>
                                     <br />
                               </#list>
                            </#list> 
                        </ul>
                        <br/>
                    </#if>
					
					<#-- transitions -->
					<p> 
					<#if node.getLeavingTransitions()?size == 1>
						<#assign afterTask = node.getLeavingTransitions()?first >
						Далее управление переходит к шагу <a href="#${afterTask.getTarget().getId()}">${afterTask.getTarget().getName()}</a>
					<#else>
						Далее управление переходит:   
						<ul>
							<#list node.getLeavingTransitions() as transition>
								<li>в случае ${transition.getName()} <a href="#${transition.getTarget().getId()}">${transition.getTarget().getName()}</a> </li>
							</#list> 
						</ul>
					</#if>
					</p> 
					
					<#-- timer option -->
					<#if node.getTimer()?? >
						<#assign timer = node.getTimer() >
						<#if timer.getLeavingTransitions()?? && ( timer.getLeavingTransitions()?size > 0) >
							<#assign afterTimer = timer.getLeavingTransitions()?first>
							<#if timer.getDelay().hasDuration() >
								После истечения ${node.getPropertyValue("timerDelay").toString()} времени управление переходит к шагу 
							<#else>
								${timerDelay.toString()} времени управление переходит к шагу
							</#if>
							<a href="#${afterTimer.getTarget().getId()}">${afterTimer.getTarget().getName()}</a>
							</p>
						</#if>
					</#if>
				</#if>
				
				<#-- ParallelGateway -->
				<#if node.class.simpleName == "ParallelGateway" || node.class.simpleName == "Fork">
					<tr>
					<#if node.getParent().getName() != proc.getName()>
						<tr><td  class="subprocess-node-header-td"><div class="subprocess-node-header-text">Действие в рамках подпроцесса "${node.getParent().getName()}"</div></td></tr>
					</#if>
					<td>
					<hr color="#e3f4ff">
				
					<#-- name -->
					<p id="${node.getId()}" > <font color="#005D85" size="3" face="Verdana, Geneva, sans-serif"> <strong> Шаг: Параллельный шлюз ${node.getName()} </strong> </font> </p>
					
					<#-- type -->
					<#if ( node.getLeavingTransitions()?size > node.getArrivingTransitions()?size ) >
						<p><span class="node-type-text-1">Тип шага:</span> Разделение<br/>
					<#else>
						<p><span class="node-type-text-1">Тип шага:</span> Слияние<br/>
					</#if>
					
					<#if node.getNodeRegulationsProperties().getDescriptionForUser()?length != 0 >
						<span class="node-description-text-1">Описание:</span> <br/>
						${node.getNodeRegulationsProperties().getDescriptionForUser()?replace("\n","<br />")}
						<br/><br/>
					</#if>
					
					<#-- transitions -->
					<#if node.getLeavingTransitions()?size == 1>
						<#assign afterTask = node.getLeavingTransitions()?first >
						Далее управление переходит к шагу <a href="#${afterTask.getTarget().getId()}">${afterTask.getTarget().getName()}</a></p>
					<#else>
						Далее управление переходит:</p>    
						<ul>
							<#list node.getLeavingTransitions() as transition>
								<p>в случае ${transition.getName()} <a href="#${transition.getTarget().getId()}">${transition.getTarget().getName()}</a> </p>
							</#list> 
						</ul>
					</#if>
				</#if>
				
				<#-- Join -->
				<#if node.class.simpleName == "Join">
					<tr>
					<#if node.getParent().getName() != proc.getName()>
						<tr><td  class="subprocess-node-header-td"><div class="subprocess-node-header-text">Действие в рамках подпроцесса "${node.getParent().getName()}"</div></td></tr>
					</#if>
					<td>
					<hr color="#e3f4ff">
					
					<#-- name -->
					<p id="${node.getId()}" > <font color="#005D85" size="3" face="Verdana, Geneva, sans-serif"> <strong> Шаг: Соединение ${node.getName()} </strong> </font> </p>
					
					<#-- type -->
					<p><span class="node-type-text-1">Тип шага:</span> Соединение<br/>
					
					<#if node.getNodeRegulationsProperties().getDescriptionForUser()?length != 0 >
						<span class="node-description-text-1">Описание:</span> <br/>
						${node.getNodeRegulationsProperties().getDescriptionForUser()?replace("\n","<br />")}
						<br/><br/>
					</#if>
					
					<#-- transitions -->
					<#assign afterNode = node.getLeavingTransitions()?first >
					Далее cоединяются ${node.getArrivingTransitions()?size} точек управления, и управление переходит к шагу
					<a href="#${afterNode.getTarget().getId()}">${afterNode.getTarget().getName()}</a> 
					</p>    
				</#if>
				
				<#-- ExclusiveGateway -->
				<#if node.class.simpleName == "ExclusiveGateway">
					<tr>
					<#if node.getParent().getName() != proc.getName()>
						<tr><td  class="subprocess-node-header-td"><div class="subprocess-node-header-text">Действие в рамках подпроцесса "${node.getParent().getName()}"</div></td></tr>
					</#if>
					<td>
					<hr color="#e3f4ff">
					
					<#-- name -->
					<p id="${node.getId()}" > <font color="#005D85" size="3" face="Verdana, Geneva, sans-serif"> <strong> Шаг:  ${node.getName()} </strong> </font> </p>
					
					<#-- type -->
					<span class="node-type-text-1">Тип шага:</span> Исключающий шлюз<br/>
					
					<#if node.getNodeRegulationsProperties().getDescriptionForUser()?length != 0 >
						<span class="node-description-text-1">Описание:</span> <br/>
						${node.getNodeRegulationsProperties().getDescriptionForUser()?replace("\n","<br />")}
						<br/><br/>
					</#if>
					
					<#-- transitions -->
					<p> 
					Далее управление переходит:   
						<ul>
							<#list node.getLeavingTransitions() as transition>
								<li>в случае ${transition.getName()} <a href="#${transition.getTarget().getId()}">${transition.getTarget().getName()}</a> </li>
							</#list> 
						</ul>
					</p> 
					</td>
				</#if>
				
				<#-- Timer -->
				<#if node.class.simpleName == "Timer" >
					<tr>
					<#if node.getParent().getName() != proc.getName()>
						<tr><td  class="subprocess-node-header-td"><div class="subprocess-node-header-text">Действие в рамках подпроцесса "${node.getParent().getName()}"</div></td></tr>
					</#if>
					<td>
					<hr color="#e3f4ff">
					
					<#-- name -->
					<p id="${node.getId()}" > <font color="#005D85" size="3" face="Verdana, Geneva, sans-serif"> <strong> Шаг: ${node.getName()} </strong> </font> </p>
					
					<#-- type -->
					<p><span class="node-type-text-1">Тип шага:</span> Таймер<br/>
					
					<#if node.getNodeRegulationsProperties().getDescriptionForUser()?length != 0 >
						<span class="node-description-text-1">Описание:</span> <br/>
						${node.getNodeRegulationsProperties().getDescriptionForUser()?replace("\n","<br />")}
						<br/><br/>
					</#if>
							
					<#-- transitions -->
					<#assign afterNode = node.getLeavingTransitions()?first >
					<#assign timerDelay = node.getPropertyValue("timerDelay") >
					<#if timerDelay.hasDuration() >
						После истечения ${node.getPropertyValue("timerDelay").toString()} времени управление переходит к шагу 
					<#else>
						${timerDelay.toString()} времени управление переходит к шагу
					</#if>
						<a href="#${afterNode.getTarget().getId()}">${afterNode.getTarget().getName()}</a>
					</p>
				</#if>
				
				<#-- Receive & Send message -->
				<#if node.class.simpleName == "ReceiveMessageNode" || node.class.simpleName == "SendMessageNode">
					<tr>
					<#if node.getParent().getName() != proc.getName()>
						<tr><td  class="subprocess-node-header-td"><div class="subprocess-node-header-text">Действие в рамках подпроцесса "${node.getParent().getName()}"</div></td></tr>
					</#if>					
					<td>
					<hr color="#e3f4ff">
					
					<#-- name -->
					<p id="${node.getId()}" > <font color="#005D85" size="3" face="Verdana, Geneva, sans-serif"> <strong> Шаг: ${node.getName()} </strong> </font> </p>
					
					<#-- type -->
					<p>
					<#if node.class.simpleName == "ReceiveMessageNode">
						<span class="node-type-text-1">Тип шага:</span> Прием сообщения<br/>
					<#else>
						<span class="node-type-text-1">Тип шага:</span> Отправка сообщения <br/>
					</#if>
					
					<#if node.getNodeRegulationsProperties().getDescriptionForUser()?length != 0 >
						<span class="node-description-text-1">Описание:</span> <br/>
						${node.getNodeRegulationsProperties().getDescriptionForUser()?replace("\n","<br />")}
						<br/><br/>
					</#if>
					
					<#-- transitions -->
					<#assign afterNode = node.getLeavingTransitions()?first >
					<#if node.class.simpleName == "ReceiveMessageNode">
						После приема сообщения управление переходит к шагу
					<#else>
						После отправки сообщения управление переходит к шагу
					</#if>
						<a href="#${afterNode.getTarget().getId()}">${afterNode.getTarget().getName()}</a>
					</p>
					
					<#-- timer option -->
					<#if node.class.simpleName == "ReceiveMessageNode"  && node.getTimer()?? >
						<#assign timer = node.getTimer() >
						<#assign afterTimer = timer.getLeavingTransitions()?first>
						<p> 
							В случае задержки задания на ${timer.getPropertyValue("timerDelay").toString()} времени управление переходит к шагу 
							<a href="#${afterTimer.getTarget().getId()}">${afterTimer.getTarget().getName()}</a>
						</p>
					</#if>
					
					<#-- msg live time option -->
					<#if node.class.simpleName == "SendMessageNode" && node.getTtlDuration() ??>
						<p> Время жизни сообщения ${node.getTtlDuration().toString()}</p>
					</#if>
				</#if>
				
				<#-- Subprocess & Multisubprocess -->
				<#if node.class.simpleName == "Subprocess" || node.class.simpleName == "MultiSubprocess">
					<tr>
					<hr color="#e3f4ff">
					<#if node.getParent().getName() != proc.getName()>
						<tr><td  class="subprocess-node-header-td"><div class="subprocess-node-header-text">Действие в рамках подпроцесса "${node.getParent().getName()}"</div></td></tr>
					</#if>					
					<td>
					
					<#-- name -->
					<p id="${node.getId()}" > <font color="#005D85" size="3" face="Verdana, Geneva, sans-serif"> <strong> Шаг: ${node.getName()} </strong> </font> </p>
					
					<#-- type -->
					<#if node.class.simpleName == "Subprocess">
						<span class="node-type-text-1">Тип шага:</span> Запуск подпроцесса
					<#else>
						<span class="node-type-text-1">Тип шага:</span> Запуск мультиподпроцесса
					</#if>
					
					<#if node.getNodeRegulationsProperties().getDescriptionForUser()?length != 0 >
						<span class="node-description-text-1">Описание:</span> <br/>
						${node.getNodeRegulationsProperties().getDescriptionForUser()?replace("\n","<br />")}
					</#if>
					
					<#-- suprocess name -->
					<#if node.getSubProcessName() != "">
					<br/><br/>
					 <span class="subprocess-name-text-1">Имя подпроцесса:</span> ${node.getSubProcessName()}
					</#if>
				</#if>
				
				<#-- Multi task state -->
				<#if node.class.simpleName == "MultiTaskState">
					<tr>
					<#if node.getParent().getName() != proc.getName()>
						<tr><td  class="subprocess-node-header-td"><div class="subprocess-node-header-text">Действие в рамках подпроцесса "${node.getParent().getName()}"</div></td></tr>
					</#if>					
					<td>
					<hr color="#e3f4ff">
					
					<#-- name -->
					<p id="${node.getId()}" > <font color="#005D85" size="3" face="Verdana, Geneva, sans-serif"> <strong> Шаг: ${node.getName()} </strong> </font> </p>
					
					<#-- type -->
					<p>
						<span class="node-type-text-1">Тип шага:</span> Запуск мультидействия <br/>
					</p>
					
					<#if node.getNodeRegulationsProperties().getDescriptionForUser()?length != 0 >
						<span class="node-description-text-1">Описание:</span> <br/>
						${node.getNodeRegulationsProperties().getDescriptionForUser()?replace("\n","<br />")}
						<br/><br/>
					</#if>
					
					<#if node.hasFormValidation() == true >
                       <#assign formNodeValidation = mapOfFormNodeValidation[node.getId()] >
                        <span class="node-validation-text-1">Валидация:</span> <br/>
                        <ul>
                         <#assign formNodeValidationGetFieldConfigs = formNodeValidation.getFieldConfigs()>
                            <#list formNodeValidationGetFieldConfigs?keys as variableName>
                            <li>Переменная &quot;${variableName}&quot;
                                <#list formNodeValidationGetFieldConfigs[variableName]?keys as nodeFieldConfigsValueKey>
                                    <ul>
                                       <li>Тип валидатора: &quot;${formNodeValidationGetFieldConfigs[variableName][nodeFieldConfigsValueKey].getType()}&quot;</li>
                                       <#if formNodeValidationGetFieldConfigs[variableName][nodeFieldConfigsValueKey].getMessage()?length != 0>
                                       <li>Сообщение валидатора: &quot;${formNodeValidationGetFieldConfigs[variableName][nodeFieldConfigsValueKey].getMessage()}&quot;</li>
                                       </#if>
                                       <#if formNodeValidationGetFieldConfigs[variableName][nodeFieldConfigsValueKey].getParams()?size != 0>
                                           <li>Параметры валидатора:
                                               <ul>
                                               <#list formNodeValidationGetFieldConfigs[variableName][nodeFieldConfigsValueKey].getParams()?keys as parameterName>
                                               <li>${parameterName} = &quot;${formNodeValidationGetFieldConfigs[variableName][nodeFieldConfigsValueKey].getParams()[parameterName]}&quot;</li>
                                               </#list>
                                               </ul>
                                           </li> 
                                       </#if>
                                        <#if formNodeValidationGetFieldConfigs[variableName][nodeFieldConfigsValueKey].getTransitionNames()?size != 0>
                                               <li> Валидатор применяется только к переходам: 
                                                      <#list formNodeValidationGetFieldConfigs[variableName][nodeFieldConfigsValueKey].getTransitionNames() as transitionName>
                                                          "${transitionName}";
                                                      </#list>
                                               </li>
                                         </#if>
                                    </ul>
                                     </li>
                                     <br />
                               </#list>
                            </#list> 
                        </ul>
                        <br/>
                    </#if>
					
					<#-- transitions -->
					<p>
					<#if node.getLeavingTransitions()?size == 1>
						<#assign afterTask = node.getLeavingTransitions()?first >
						Далее управление переходит к шагу <a href="#${afterTask.getTarget().getId()}">${afterTask.getTarget().getName()}</a>
					<#else>
						Далее управление переходит: 
						<ul>
							<#list node.getLeavingTransitions() as transition>
								<li>в случае ${transition.getName()} <a href="#${transition.getTarget().getId()}">${transition.getTarget().getName()}</a> </li>
							</#list> 
						</ul>
					</#if>
					</p>	
					
				</#if>
				
				<#--  ScriptTask -->
				<#if node.class.simpleName == "ScriptTask">
					<tr>
					<#if node.getParent().getName() != proc.getName()>
						<tr><td  class="subprocess-node-header-td"><div class="subprocess-node-header-text">Действие в рамках подпроцесса "${node.getParent().getName()}"</div></td></tr>
					</#if>					
					<td>
					<hr color="#e3f4ff">
					
					<#-- name -->
					<p id="${node.getId()}" > <font color="#005D85" size="3" face="Verdana, Geneva, sans-serif"> <strong> Шаг: ${node.getName()} </strong> </font> </p>
					
					<#-- type -->
					<p><span class="node-type-text-1">Тип шага:</span> Выполнение сценария</p>
					
					<#if node.getNodeRegulationsProperties().getDescriptionForUser()?length != 0 >
						<span class="node-description-text-1">Описание:</span> <br/>
						${node.getNodeRegulationsProperties().getDescriptionForUser()?replace("\n","<br />")}
						<br/><br/>
					</#if>
					
					<#-- transitions -->
					<p>
					<#if node.getLeavingTransitions()?size == 1>
						<#assign afterTask = node.getLeavingTransitions()?first >
						Далее управление переходит к шагу <a href="#${afterTask.getTarget().getId()}">${afterTask.getTarget().getName()}</a>
					<#else>
						Далее управление переходит:  
						<ul>
							<#list node.getLeavingTransitions() as transition>
								<li>в случае ${transition.getName()} <a href="#${transition.getTarget().getId()}">${transition.getTarget().getName()}</a> </li>
							</#list> 
						</ul>
					</#if>
					</p>
				</#if>
				
				<#--  EndTokenState -->
				<#if node.class.simpleName == "EndTokenState">
					<tr>
					<#if node.getParent().getName() != proc.getName()>
						<tr><td  class="subprocess-node-header-td"><div class="subprocess-node-header-text">Действие в рамках подпроцесса "${node.getParent().getName()}"</div></td></tr>
					</#if>					
					<td><hr color="#e3f4ff">
					<p id="${node.getId()}"> Завершение потока  выполнения бизнес-процесса "${node.getParent().getName()}": ${node.getName()} </p>
					<#if node.getNodeRegulationsProperties().getDescriptionForUser()?length != 0 >
						<span class="node-description-text-1">Описание:</span> <br/>
						${node.getNodeRegulationsProperties().getDescriptionForUser()?replace("\n","<br />")}
						<br/><br/>
					</#if>
					</td></tr>
				</#if>
				
			    <#--  EndState -->
				<#if node.class.simpleName == "EndState">
					<tr>
					<#if node.getParent().getName() != proc.getName()>
						<tr><td  class="subprocess-node-header-td"><div class="subprocess-node-header-text">Действие в рамках подпроцесса "${node.getParent().getName()}"</div></td></tr>
					</#if>					
					<td><hr color="#e3f4ff">
					<p id="${node.getId()}"> Завершение процесса выполнения бизнес-процесса "${node.getParent().getName()}": ${node.getName()} </p>
					<#if node.getNodeRegulationsProperties().getDescriptionForUser()?length != 0 >
						<span class="node-description-text-1">Описание:</span> <br/>
						${node.getNodeRegulationsProperties().getDescriptionForUser()?replace("\n","<br />")}
						<br/><br/>
					</#if>
					</td></tr>
				</#if>
			
				</td></tr>	
			</#if>
		
		</#list>		
		</table>	
		
			
	</body>
</html>