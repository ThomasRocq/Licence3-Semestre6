
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#include "common.h"
#include "question.h"
#include "packet.h"



void init(){
    reserved[0] = 0x00;
    reserved[1] = 0x00;
    reserved[2] = 0x00;
    r_code = NO_ERROR;
    an_count = 0;
    ns_count = 0;
    ar_count = 0;
    recursion_available = 0;
    authoritative_answer = 0;
}

void set_id(unsigned char b1, unsigned char b2){
    id[0] = b1;
    id[1] = b2;
}

void set_qerry_type(unsigned char type){
    assert_message( (type == 0 || type == 1), "Rrror: cannot set type of packet");
    qr = type;
}

void set_op_code(type_of_request op){
    op_code = op;
}

void set_authoritative_answer(unsigned char aa){
    assert_message(qr != 0, "The type of packet is not answer.");
    authoritative_answer = aa;
}

void set_truncated(unsigned char t){
    assert_message( (t == 0 || t == 1), "Error cannot set TrunCation");
    tc = t;
}

void set_recursion_desired(unsigned char rd){
    assert_message( (rd == 0 || rd == 1), "Error cannot set recursion desired");
    recursion_desired = rd;
}

void set_recursion_available(unsigned char ra){
    assert_message(qr != 0, "The type of packet is not answer.");
    recursion_available = ra;
}

void create_simple_query(char* hostname){
    init();
    set_id(((unsigned char)(rand() & 0x00f)), ((unsigned char)(rand() & 0x00ff)));
    set_qerry_type(0);
    set_op_code(QUERY);
    set_truncated(0);
    set_recursion_desired(1);
    add_question(hostname, A, IN);
}

void display_packet(){
    printf("DNS packet details :\n");
	printf("\tTransaction Id: %s\n", id);
	printf("\tType of packet: %i\n", qr);
	printf("\t The OpCode : %i\n", op_code);
	if (qr == 1) {
        printf("\tIs authoritative: %i\n", authoritative_answer);
    }
	printf("\tIs truncated: %i\n", tc);
	printf("\tIs recursion desired: %i\n", recursion_desired);
	if (qr == 1 ) {
		printf("\tIs recursion available: %i\n", recursion_available);
    }
	if (qr == 0) {
		printf("\tResponse code: %d\n"+r_code);
    }

    printf("\tNumber of query: %i\n", get_index_question() );

    display_questions();
}

void  get_bytes_query(char buffer[16384]){
    int index_of_buffer = 0;
    int i=0, j = 0, offset = 0;
    unsigned char buffer_byte[8] ={0,0,0,0,0,0,0,0};
    char buffer_label[512];
    unsigned char bits[8];

    /* Insert id */
    buffer[index_of_buffer++] = id[0];
    buffer[index_of_buffer++] = id[1];

    /* Insert  1er flags*/
    int_to_byte(op_code, buffer_byte);
    bits[0] = qr;
    bits[1] = buffer_byte[4];
    bits[2] = buffer_byte[5];
    bits[3] = buffer_byte[6];
    bits[4] = buffer_byte[7];
    bits[5] = authoritative_answer;
    bits[6] = tc;
    bits[7] = recursion_desired;
    buffer[index_of_buffer++] = toByte(bits);
    memset(&buffer_byte[0], 0, sizeof(buffer_byte));
    memset(&bits[0], 0, sizeof(bits));


    /* Insert  2e flags*/
    int_to_byte(r_code, buffer_byte);
    bits[0] = recursion_available;
    bits[1] = 0; /* Insert Z (for more info see rfc1035.txt line 1473)*/
    bits[2] = 0; /* Insert Z (for more info see rfc1035.txt line 1473)*/
    bits[3] = 0; /* Insert Z (for more info see rfc1035.txt line 1473)*/
    bits[4] = buffer_byte[4];
    bits[5] = buffer_byte[5];
    bits[6] = buffer_byte[6];
    bits[7] = buffer_byte[7];
    buffer[index_of_buffer++] = toByte(bits);
    memset(&buffer_byte[0], 0, sizeof(buffer_byte));
    memset(&bits[0], 0, sizeof(bits));

    /* Insert QDCOUNT */
    buffer[index_of_buffer++] = ( get_index_question() >> 8 ) & 0xffff;
    buffer[index_of_buffer++] = get_index_question() & 0xffff;

    /* Insert an_count */
    buffer[index_of_buffer++] = ( an_count >> 8 ) & 0xffff;
    buffer[index_of_buffer++] = an_count & 0xffff;
    /* Insert ns_count */
    buffer[index_of_buffer++] = (ns_count >> 8) & 0xffff;
    buffer[index_of_buffer++] = ns_count & 0xffff;
    /* Insert ar_count */
    buffer[index_of_buffer++] = ( ar_count >> 8) & 0xffff;
    buffer[index_of_buffer++] = ar_count & 0xffff;

    /* Insert all Questions */
    for(i=0; i< get_index_question(); i++){
        offset = convert_donmaine_name_to_label(get_name(i), buffer_label);
        for( j = 0; j < offset; j++){
            buffer[index_of_buffer++] = buffer_label[j];
        }
        buffer[index_of_buffer++] = (get_type(i) >> 8) & 0xffff;
        buffer[index_of_buffer++] = get_type(i) & 0xffff;

        buffer[index_of_buffer++] = ( get_class(i) >> 8) & 0xffff;
        buffer[index_of_buffer++] = get_class(i) & 0xffff;
    }

    for (i = 0; i < index_of_buffer; i++) {
        printf("buffer[%i] = %04x\n",i, buffer[i]);
    }
}
