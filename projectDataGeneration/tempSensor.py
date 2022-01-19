import time
import sys
import pika
import random
import json

class TempSensor:
    def __init__(self, id, start_temp = None, sleep_time = 10):
        if start_temp != None:
            self.base_temp = start_temp
        else:
            self.base_temp = random.uniform(12,17)
        self.sleep_time = sleep_time
        self.type = "Temperature"
        self.id = id
        self.value = self.base_temp
        self.power = random.uniform(30,40)                             #Not sure if values are like real life, unit is Watts per hour
        self.credentials = pika.PlainCredentials('guest', 'guest')
        self.connection = pika.BlockingConnection(pika.ConnectionParameters(host='172.18.0.7', credentials=self.credentials))
        #self.connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost',port=5672, credentials=self.credentials))

        self.channel = self.connection.channel()
        self.queue = 'temperature_queue'
        self.channel.queue_declare(queue=self.queue, durable=True)

    def run(self):
        temp = self.base_temp
        while True:
            temp_change = random.random() / 4    #variação de temperatura
            chance = random.random()             
            temp_dif = temp-self.base_temp      #diferença entre temperatura inicial e temperatura atual
            up_or_down = (0.5 + (0.5 * (temp_dif/3)))     #regra dos 3 simples para ter uma diferença máxima de 3 da base_temp

            #Exemplo:
            #           base_temp = 13  temp = 14.5 temp_dif = -1.5 max_dif = 3
            #           1 --------- 3
            #           x --------- -1.5    -->  (-1.5*1)/3 = -0.5
            #           up_or_down = 0.5 + (0.5*-0.5) = 0.25
            # Quando a dif é 3, x vai ser 1, logo up_or_down = 0.5 + 0.5*1 = 1
            # Quando a dif é -3, x vai ser -1, logo up_or_down = 0.5 + 0.5*-1 = 0

            if chance > up_or_down:
                temp = temp + temp_change
            else:
                temp = temp - temp_change
            
            self.value = temp
            power = ((self.power / 60)/60) * self.sleep_time
            print(self.value)
            print(power)
            message = {"id":self.id, "value":self.value, "power":power}
            self.channel.basic_publish(
                exchange = '',
                routing_key = self.queue, 
                body = json.dumps(message),
                properties=pika.BasicProperties(delivery_mode=pika.spec.PERSISTENT_DELIVERY_MODE),
            )
            time.sleep(self.sleep_time)

if __name__ == '__main__':
    id = sys.argv[1]
    temp = TempSensor(id)
    temp.run()

    """temp = TempSensor(1,None,1)
    temp.run()"""
    
